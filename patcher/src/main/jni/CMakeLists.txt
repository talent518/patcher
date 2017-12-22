cmake_minimum_required(VERSION 3.4.1)
add_library(Patcher SHARED blocksort.c huffman.c crctable.c randtable.c compress.c decompress.c bzlib.c bspatch.c Patcher.c)
find_library(Patcher android log z m)
target_link_libraries(Patcher android log z m)
