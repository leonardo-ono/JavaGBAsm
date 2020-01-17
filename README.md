# JavaGBAsm

Very simple GameBoy assembler (CPU‎: ‎Sharp LR35902) implemented in java + Hello World assembly example program.

Video: https://youtu.be/-_QCDYcExfw

Hello World example program: https://github.com/leonardo-ono/JavaGBAsm/blob/master/example/hello.asm

Usage: java -jar jgbasm.jar hello.asm hello.gb

Some details about this assembler:
 - can generate only 32Kb 'ROM ONLY' (00h) Cartridge Type 
 - can't evaluate expressions
 - labels must have at least 3 bytes
 - support non local + local labels (similar to nasm)
 - supported pseudo instructions: org, equ & db
 - decimal literal numbers: 127, 10
 - hex literal numbers: $ab, 0abh
 - binary literal numbers: %10010001, 10010001b
 
 Requires java 8+.
 
 
