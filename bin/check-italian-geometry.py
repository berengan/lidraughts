#!/usr/bin/env python3
"""Regression oracle for the Italian 8x8 board (1..32).

Coordinates use top-left as (0, 0), with the first playable dark
square at (0, 0). The model deliberately does not depend on UI or Scala.
"""
from __future__ import annotations

BOARD_SIZE = 8
FIELDS = 32


def square(field: int) -> tuple[int, int]:
    if not 1 <= field <= FIELDS:
        raise ValueError(f"Invalid Italian field: {field}")
    index = field - 1
    row, half_column = divmod(index, 4)
    return (2 * half_column + row % 2, row)


def field_at(column: int, row: int) -> int | None:
    if not (0 <= column < BOARD_SIZE and 0 <= row < BOARD_SIZE):
        return None
    if (column + row) % 2 != 0:
        return None
    return 4 * row + (column - row % 2) // 2 + 1


def diagonal_neighbors(field: int) -> set[int]:
    column, row = square(field)
    return {
        found
        for dx in (-1, 1)
        for dy in (-1, 1)
        if (found := field_at(column + dx, row + dy)) is not None
    }


def is_diagonal_step(start: int, end: int) -> bool:
    x1, y1 = square(start)
    x2, y2 = square(end)
    return abs(x2 - x1) == abs(y2 - y1) == 1


def verify() -> None:
    coordinates = [square(field) for field in range(1, FIELDS + 1)]
    assert len(set(coordinates)) == FIELDS
    assert all((x + y) % 2 == 0 for x, y in coordinates)
    assert all(field_at(*square(field)) == field for field in range(1, FIELDS + 1))
    assert field_at(0, 0) == 1
    assert field_at(7, 7) == 32
    assert is_diagonal_step(23, 19)
    assert is_diagonal_step(11, 15)
    assert not is_diagonal_step(19, 16)
    for field in range(1, FIELDS + 1):
        for neighbor in diagonal_neighbors(field):
            assert field in diagonal_neighbors(neighbor)
            assert is_diagonal_step(field, neighbor)
    print("OK: 32 Italian squares, dark top-left/bottom-right")
    print("OK: coordinate roundtrip and symmetric diagonal adjacency")
    print("OK: 23-19 and 11-15 diagonal; 19-16 NOT diagonal")
    print("Diagonal neighbors:")
    for field in range(1, FIELDS + 1):
        print(f"{field:02d}: {','.join(f'{n:02d}' for n in sorted(diagonal_neighbors(field)))}")


if __name__ == "__main__":
    verify()
