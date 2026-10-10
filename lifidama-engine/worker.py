#!/usr/bin/env python3
"""Experimental LiFiDama Italian-draughts Draughtsnet worker.

Dry-run by default. Never posts capture moves until the 'taken' protocol is verified.
"""
import argparse
import json
import logging
import os
import re
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path

ENGINE = "LiFiDama-Italian"
LEVELS = {1:"beginner",2:"beginner",3:"intermediate",4:"intermediate",
          5:"professional",6:"professional",7:"ultra",8:"ultra"}
MOVE = re.compile(r"^bestmove=(\d{1,2})([-x])(\d{1,2})$", re.M)
LOG = logging.getLogger("lifidama-worker")

def normalize_fen(fen):
    if not isinstance(fen, str):
        raise ValueError("missing currentFen")
    fen = fen.strip()
    if not re.fullmatch(r"[WB]:(?:[BW][K0-9,\-]*:)?[BW][K0-9,\-]*", fen):
        raise ValueError("unsupported currentFen format")
    if fen.count(":W") != 1 or fen.count(":B") != 1:
        raise ValueError("expected one section per color")
    for n in re.findall(r"(?<![A-Za-z])\d+", fen):
        if int(n) < 1 or int(n) > 32:
            raise ValueError("square outside 32-square Italian board")
    return fen

def level_for(value):
    if type(value) is not int or value not in LEVELS:
        raise ValueError("invalid level")
    return LEVELS[value]

def parse_bestmove(output):
    match = MOVE.search(output)
    if not match:
        raise ValueError("engine returned no bestmove")
    a, sep, b = match.groups()
    if not (1 <= int(a) <= 32 and 1 <= int(b) <= 32) or a == b:
        raise ValueError("invalid move")
    return f"{int(a):02d}{int(b):02d}", sep

def compute(engine, config, fen, level):
    cmd = [str(engine), "--move", "--fen", normalize_fen(fen),
           "--level", level_for(level), "--config", str(config)]
    result = subprocess.run(cmd, capture_output=True, text=True,
                            timeout=8, check=True)
    uci, sep = parse_bestmove(result.stdout)
    if sep == "x":
        raise ValueError("capture 'taken' encoding unverified; refusing to post")
    return {"bestmove":uci, "taken":""}

def request(base, path, body):
    req = urllib.request.Request(base.rstrip("/") + path,
        data=json.dumps(body).encode("utf-8"),
        headers={"Content-Type":"application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=10) as res:
            if res.status == 204:
                return None
            if res.status != 202:
                raise RuntimeError(f"unexpected HTTP {res.status}")
            return json.load(res)
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"HTTP {e.code} on {path}") from e

def validate_work(job):
    if job.get("work",{}).get("type") != "move":
        raise ValueError("not a move job")
    if job.get("variant") != "italian":
        raise ValueError("received non-Italian job")
    if not re.fullmatch(r"[A-Za-z0-9]{8}",str(job["work"].get("id",""))):
        raise ValueError("invalid work ID")
    return normalize_fen(job.get("currentFen")), level_for(job["work"].get("level"))

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--url",default=os.getenv("LIFIDAMA_URL","http://127.0.0.1:9663"))
    p.add_argument("--key",default=os.getenv("LIFIDAMA_DRAUGHTSNET_KEY"))
    p.add_argument("--engine",type=Path,default=Path("./dama-linux"))
    p.add_argument("--config",type=Path,default=Path("./engine-levels.ini"))
    p.add_argument("--poll",type=float,default=2.0)
    p.add_argument("--once",action="store_true")
    p.add_argument("--post",action="store_true")
    args=p.parse_args()
    logging.basicConfig(level=logging.INFO)
    if not args.key: p.error("missing --key or LIFIDAMA_DRAUGHTSNET_KEY")
    if args.poll<1: p.error("--poll must be >= 1")
    if args.post and args.once:
        p.error("--once and --post cannot be combined: --once is a diagnostic")
    if not args.engine.is_file() or not args.config.is_file():
        p.error("missing engine/config file")
    meta={"draughtsnet":{"version":"1.0.0","apikey":args.key},
          "scan":{"name":ENGINE}}
    while True:
        try:
            job=request(args.url,"/draughtsnet/acquire",meta)
            if job:
                fen,level=validate_work(job)
                result=compute(args.engine,args.config,fen,job["work"]["level"])
                LOG.info("job=%s level=%s move=%s",job["work"]["id"],level,result)
                if args.post:
                    request(args.url,"/draughtsnet/move/"+job["work"]["id"],
                            {**meta,"move":result})
            elif args.once: LOG.info("no work available")
        except (ValueError,RuntimeError,subprocess.CalledProcessError,
                subprocess.TimeoutExpired,urllib.error.URLError) as e:
            LOG.error("%s",e)
        if args.once: break
        time.sleep(args.poll)

if __name__=="__main__": main()
