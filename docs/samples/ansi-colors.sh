#!/usr/bin/env bash
# Print the normal and bright ANSI palette in an IDE terminal.
set -euo pipefail
names=(black red green yellow blue magenta cyan gray)
for i in "${!names[@]}"; do
    printf '\033[%dm%-10s\033[0m \033[%dmbright %s\033[0m\n' \
        "$((30 + i))" "${names[$i]}" "$((90 + i))" "${names[$i]}"
done
