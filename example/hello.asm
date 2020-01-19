		VRAM equ $8000

		org $100
		
entry_point:
		nop
		jp start
		
		org $150
		
start:
		; according to http://bgb.bircd.org/pandocs.htm#videodisplay:
		; "CAUTION: Stopping LCD operation (Bit 7 from 1 to 0) may be 
		; performed during V-Blank ONLY, disabeling the display 
		; outside of the V-Blank period may damage the hardware."
		call wait_for_vblank
		
		; disable display
		ld a, 0
		ldh ($40), a
		
		; load "HELLO WORLD!!!" characters
		ld hl, chars_data ; src
		ld bc, VRAM ; dst
		ld d, 240 ; size
		call memcpy
		
		; print "HELLO WORLD!!!"
		ld hl, screen_data ; src
		ld bc, $9900 ; dst
		ld d, 50 ; size
		call memcpy
		
		; enable display
		ld a, %10010001
		ldh ($40), a
		
	.end:
		nop
		jp .end
		
wait_for_vblank:
		ldh a, ($44) ; LY
		cp 144
		jr nz, wait_for_vblank
		ret
	
memcpy:
	.next_byte:
		ld a, (hl+)
		ld (bc), a
		inc bc
		dec d
		jr nz, .next_byte
		ret
		
chars_data: ; 15 * 16 = 240 bytes
	db $00,$00,$00,$00,$00,$00,$00,$00
	db $00,$00,$00,$00,$00,$00,$00,$00
	db $84,$84,$84,$84,$84,$84,$FC,$FC
	db $84,$84,$84,$84,$84,$84,$00,$00
	db $00,$00,$70,$70,$D8,$D8,$F8,$F8
	db $F0,$F0,$60,$60,$3C,$3C,$00,$00
	db $60,$60,$20,$20,$20,$20,$20,$20
	db $30,$30,$10,$10,$18,$18,$00,$00
	db $20,$20,$10,$10,$10,$10,$10,$10
	db $10,$10,$10,$10,$10,$10,$00,$00
	db $00,$00,$3C,$3C,$64,$64,$44,$44
	db $44,$44,$64,$64,$3C,$3C,$00,$00
	db $00,$00,$00,$00,$00,$00,$00,$00
	db $00,$00,$00,$00,$00,$00,$00,$00
	db $02,$02,$82,$82,$82,$82,$96,$96
	db $D4,$D4,$7C,$7C,$00,$00,$00,$00
	db $00,$00,$7C,$7C,$C4,$C4,$84,$84
	db $CC,$CC,$78,$78,$00,$00,$00,$00
	db $00,$00,$40,$40,$7C,$7C,$60,$60
	db $40,$40,$60,$60,$20,$20,$00,$00
	db $38,$38,$28,$28,$28,$28,$28,$28
	db $38,$38,$3C,$3C,$64,$64,$00,$00
	db $10,$10,$18,$18,$08,$08,$08,$08
	db $7C,$7C,$44,$44,$7E,$7E,$18,$18
	db $04,$04,$08,$08,$18,$18,$10,$10
	db $10,$10,$30,$30,$00,$00,$40,$40
	db $0C,$0C,$08,$08,$08,$08,$18,$18
	db $10,$10,$10,$10,$00,$00,$20,$20
	db $04,$04,$0C,$0C,$18,$18,$10,$10
	db $10,$10,$20,$20,$00,$00,$40,$40

screen_data:
	db $00,$01,$02,$03,$04,$05,$06,$07
	db $08,$09,$0A,$0B,$0C,$0D,$0E,$00
	
		
		
		
		
		
		
		
		
		
		
		
	
		
		
		
		
		
		