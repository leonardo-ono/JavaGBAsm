    org 0

        JOY_PAD equ $ff00
        RAM equ 0c000h
        VRAM equ 9800h

        SCY equ 0c002h

    org 100h

entry_point:
        nop
        jp start

    org $104

nintendo_logo:
        db $CE, $ED, $66, $66, $CC, $0D, $00, $0B
        db $03, $73, $00, $83, $00, $0C, $00, $0D
        db $00, $08, $11, $1F, $88, $89, $00, $0E
        db $DC, $CC, $6E, $E6, $DD, $DD, $D9, $99
        db $BB, $BB, $67, $63, $6E, $0E, $EC, $CC
        db $DD, $DC, $99, $9F, $BB, $B9, $33, $3E
    
    org $14d

check_sum:
        ; header checksum
        db $E7

        ; global checksum
        db $67, $8B

    org 150h

start:
        ; display setup
        ld a, %10010001 ; = 91h
        ldh (40h), a

        call clear_screen

        ld hl, characters_data ; src 
        ld bc, $8000 ; dst
        ld d, 240
        call move_data

        ld hl, background_data ; src 
        ld bc, 9800h ; dst
        ld d, 14
        call move_data
   
loop:
        call fill_char

         ; scroll bkg y
        ;ld a, (SCY)
        ;ld (0ff42h), a
        ;inc a
        ;ld (SCY), a

        ; stop
        jp loop

fill_char:
        ld a, 20h
        ld (JOY_PAD), a
        ld a, (JOY_PAD)
        ld a, (JOY_PAD)
        cpl
        and 0fh
        ; swap a
        ld b, a
        ld a, 10h
        ld (JOY_PAD), a
        ld a,(JOY_PAD)
        ld a,(JOY_PAD)
        ld a,(JOY_PAD)
        ld a,(JOY_PAD)
        ld a,(JOY_PAD)
        ld a,(JOY_PAD)
        cpl
        and 0fh
        or b
        ld (9800h), a
        ;inc a
        ;ld (RAM), a
        ret

clear_screen:
        ld hl, 9800h
        ld b, 32
    .next_row:
        ld c, 32
    .next_col:
        call wait_for_video_write
        ld a, 0
        ld (hl+), a
        dec c
        jr nz, .next_col
        dec b
        jr nz, .next_row
        ret

; in: hl = src 
;     bc = dst
;      d = size
move_data:
    .next_byte:
        call wait_for_video_write
        ld a, (hl+)
        ld (bc), a
        inc bc
        dec d
        jr nz, .next_byte
        ret

wait_for_video_write:
        ldh a, ($41)
        and 2
        jr nz, wait_for_video_write
        ret

    org 7000h

characters_data: ; 240 bytes
        db $00,$00,$00,$00,$00,$00,$00,$00
        db $00,$00,$00,$00,$00,$00,$00,$00
        db $08,$08,$4C,$4C,$64,$64,$24,$24
        db $3C,$3C,$24,$24,$26,$26,$00,$00
        db $00,$00,$1E,$1E,$16,$16,$3C,$3C
        db $20,$20,$24,$24,$3C,$3C,$00,$00
        db $20,$20,$30,$30,$10,$10,$10,$10
        db $10,$10,$10,$10,$10,$10,$00,$00
        db $10,$10,$10,$10,$10,$10,$10,$10
        db $10,$10,$10,$10,$18,$18,$00,$00
        db $00,$00,$38,$38,$68,$68,$48,$48
        db $68,$68,$38,$38,$00,$00,$00,$00
        db $00,$00,$00,$00,$00,$00,$00,$00
        db $00,$00,$00,$00,$00,$00,$00,$00
        db $00,$00,$04,$04,$44,$44,$54,$54
        db $74,$74,$3C,$3C,$00,$00,$00,$00
        db $00,$00,$3C,$3C,$64,$64,$44,$44
        db $44,$44,$64,$64,$3C,$3C,$00,$00
        db $00,$00,$1C,$1C,$50,$50,$50,$50
        db $60,$60,$20,$20,$20,$20,$00,$00
        db $18,$18,$38,$38,$28,$28,$38,$38
        db $18,$18,$78,$78,$0C,$0C,$00,$00
        db $08,$08,$08,$08,$0C,$0C,$34,$34
        db $7C,$7C,$44,$44,$6E,$6E,$3A,$3A
        db $04,$04,$0C,$0C,$08,$08,$08,$08
        db $18,$18,$10,$10,$00,$00,$20,$20
        db $04,$04,$0C,$0C,$08,$08,$18,$18
        db $10,$10,$00,$00,$20,$20,$00,$00
        db $08,$08,$08,$08,$08,$08,$18,$18
        db $10,$10,$10,$10,$00,$00,$20,$20

background_data:
        db $01,$02,$03,$04,$05,$06,$07,$08
        db $09,$0A,$0B,$0C,$0C,$0D

