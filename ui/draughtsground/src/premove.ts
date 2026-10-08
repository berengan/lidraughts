import * as cg from './types'
import { field2key, movesDown100, movesUp100, movesHorizontal100, movesDown64, movesUp64, movesHorizontal64 } from './util'

export default function premove(pieces: cg.Pieces, boardSize: cg.BoardSize, key: cg.Key, variant?: string, flipFiles: boolean = false): cg.Key[] {

  const piece = pieces[key],
    field: number = Number(key);

  if (piece === undefined || isNaN(field)) return new Array<cg.Key>();

  if (flipFiles && boardSize[0] === 8 && boardSize[1] === 8) {
    const row = Math.floor((field - 1) / 4) + 1;
    const col = 2 * ((field - 1) % 4) + (row % 2 === 1 ? 0 : 1);
    const result: cg.Key[] = [];
    const target = (x: number, y: number): cg.Key | undefined => {
      if (x < 0 || x >= 8 || y < 1 || y > 8 || (x + y - 1) % 2 !== 0) return undefined;
      return field2key((y - 1) * 4 + Math.floor(x / 2) + 1);
    };
    const directions = [[-1, -1], [1, -1], [-1, 1], [1, 1]];
    directions.forEach(d => {
      const dx = d[0], dy = d[1];
      const adjacent = target(col + dx, row + dy);
      if (!adjacent) return;
      if (piece.role === 'king' || (piece.role === 'man' &&
          ((piece.color === 'white' && dy === -1) || (piece.color === 'black' && dy === 1)))) {
        result.push(adjacent);
      }
      const blocker = pieces[adjacent];
      const landing = target(col + 2 * dx, row + 2 * dy);
      if (landing && (!blocker || blocker.color !== piece.color)) result.push(landing);
    });
    return result;
  }

  const frisianVariant = variant && (variant === "frisian" || variant === "frysk"),
    is100 = boardSize[0] === 10,
    movesUp = is100 ? movesUp100 : movesUp64,
    movesDown = is100 ? movesDown100 : movesDown64,
    movesHorizontal = is100 ? movesHorizontal100 : movesHorizontal64;

  const dests: cg.Key[] = new Array<cg.Key>();
  switch (piece.role) {

    case 'man':

      //
      //It is always impossible to premove a capture if the first field in that direction contains a piece of our own color:
      //enemy pieces can never land there because you only take pieces from the board after capture sequence is completed
      //

      for (let i = 0; i < (frisianVariant ? 3 : 2); i++) {
        let f = movesUp[field][i];
        if (f != -1) {

          const key = field2key(f);
          if (piece.color === 'white' && i < 2)
            dests.push(key);

          const pc = pieces[key];
          if (pc === undefined || pc.color !== piece.color) {
            f = movesUp[f][i];
            if (f !== -1)
              dests.push(field2key(f));
          }

        }
      }

      for (let i = 0; i < (frisianVariant ? 3 : 2); i++) {
        let f = movesDown[field][i];
        if (f != -1) {

          const key = field2key(f);
          if (piece.color === 'black' && i < 2)
            dests.push(key);

          const pc = pieces[key];
          if (pc === undefined || pc.color !== piece.color) {
            f = movesDown[f][i];
            if (f !== -1)
              dests.push(field2key(f));
          }

        }
      }

      if (frisianVariant) {
        for (let i = 0; i < 2; i++) {
          let f = movesHorizontal[field][i];
          if (f != -1) {

            const pc = pieces[field2key(f)];
            if (pc === undefined || pc.color !== piece.color) {
              f = movesHorizontal[f][i];
              if (f !== -1)
                dests.push(field2key(f));
            }

          }
        }
      }

      break;

    case 'king':

      //
      //As far as I can tell there is no configuration of pieces that makes any square theoretically impossible to be premovable 
      //

      for (let i = 0; i < (frisianVariant ? 3 : 2); i++) {
        let f = movesUp[field][i], k = 0;
        while (f != -1) {
          if (i < 2 || k > 0)
            dests.push(field2key(f));
          f = movesUp[f][i];
          k++;
        }
      }

      for (let i = 0; i < (frisianVariant ? 3 : 2); i++) {
        let f = movesDown[field][i], k = 0;
        while (f != -1) {
          if (i < 2 || k > 0)
            dests.push(field2key(f));
          f = movesDown[f][i];
          k++;
        }
      }

      if (frisianVariant) {
        for (let i = 0; i < 2; i++) {
          let f = movesHorizontal[field][i], k = 0;
          while (f != -1) {
            if (k > 0)
              dests.push(field2key(f));
            f = movesHorizontal[f][i];
            k++;
          }
        }
      }

      break;

  }

  return dests;
};
