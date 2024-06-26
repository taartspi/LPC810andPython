#!/usr/bin/python

import smbus
import math
#import utime
import smbus
import time
from ctypes import c_short

## to execute
#python

#import is31fl3731_smbus
#
#bus_num = 1
#display = is31fl3731_smbus.Matrix(bus_num)
#display.fill(127)




class const(object):
    def __init__(self, val):
        super(const, self).__setattr__("value", val)
    def __setattr__(self, name, val):
        raise ValueError("Trying to change a constant value", self)





class Matrix:
    width = 16
    height = 9
    _MODE_REGISTER = 0x00
    _FRAME_REGISTER = 0x01
    _AUTOPLAY1_REGISTER = 0x02
    _AUTOPLAY2_REGISTER = 0x03
    _BLINK_REGISTER = 0x05
    _AUDIOSYNC_REGISTER = 0x06
    _BREATH1_REGISTER = 0x08
    _BREATH2_REGISTER = 0x09
    _SHUTDOWN_REGISTER = 0x0a
    _GAIN_REGISTER = 0x0b
    _ADC_REGISTER = 0x0c

    _CONFIG_BANK = 0x0b
    _BANK_ADDRESS = 0xfd

    _PICTURE_MODE = 0x00
    _AUTOPLAY_MODE = 0x08
    _AUDIOPLAY_MODE = 0x18

    _ENABLE_OFFSET = 0x00
    _BLINK_OFFSET = 0x12
    _COLOR_OFFSET = 0x24


    def __init__(self, bus, address=0x74):
        self.bus = bus
        self.i2c = smbus.SMBus(self.bus) # Rev 2 Pi uses 1
        self.address = address
        self.reset()
        self.init()
                
    def _bank(self, bank=None):
        if bank is None:
            return self.i2c.read_i2c_block_data(self.address, self._BANK_ADDRESS, 1)[0]
        self.i2c.write_i2c_block_data(self.address, self._BANK_ADDRESS, [bank]) #bytearray([bank]))

    def _register(self, bank, register, value=None):
        self._bank(bank)
        if value is None:
            return self.i2c.read_i2c_block_data(self.address, register, 1)[0]
        self.i2c.write_i2c_block_data(self.address, register, [value])

    def _mode(self, mode=None):
        return self._register(self._CONFIG_BANK, self._MODE_REGISTER, mode)

    def init(self):
        self._mode(self._PICTURE_MODE)
        self.frame(0)
        for frame in range(8):
            self.fill(0, False, frame=frame)
            for col in range(18):
                self._register(frame, self._ENABLE_OFFSET + col, 0xff)
        self.audio_sync(False)

    def reset(self):
        self.sleep(True)
        time.sleep(0.00001)
        #utime.sleep_us(10)
        self.sleep(False)

    def sleep(self, value):
        return self._register(self._CONFIG_BANK, self._SHUTDOWN_REGISTER, not value)

    def autoplay(self, delay=0, loops=0, frames=0):
        if delay == 0:
            self._mode(self._PICTURE_MODE)
            return
        delay //= 11
        if not 0 <= loops <= 7:
            raise ValueError("Loops out of range")
        if not 0 <= frames <= 7:
            raise ValueError("Frames out of range")
        if not 1 <= delay <= 64:
            raise ValueError("Delay out of range")
        self._register(self._CONFIG_BANK, self._AUTOPLAY1_REGISTER, loops << 4 | frames)
        self._register(self._CONFIG_BANK, self._AUTOPLAY2_REGISTER, delay % 64)
        self._mode(self._AUTOPLAY_MODE | self._frame)

    def fade(self, fade_in=None, fade_out=None, pause=0):
        if fade_in is None and fade_out is None:
            self._register(self._CONFIG_BANK, self._BREATH2_REGISTER, 0)
        elif fade_in is None:
            fade_in = fade_out
        elif fade_out is None:
            fade_out = fade_in
        fade_in = int(math.log(fade_in / 26, 2))
        fade_out = int(math.log(fade_out / 26, 2))
        pause = int(math.log(pause / 26, 2))
        if not 0 <= fade_in <= 7:
            raise ValueError("Fade in out of range")
        if not 0 <= fade_out <= 7:
            raise ValueError("Fade out out of range")
        if not 0 <= pause <= 7:
            raise ValueError("Pause out of range")
        self._register(self._CONFIG_BANK, self._BREATH1_REGISTER, fade_out << 4 | fade_in)
        self._register(self._CONFIG_BANK, self._BREATH2_REGISTER, 1 << 4 | pause)

    def frame(self, frame=None, show=True):
        if frame is None:
            return self._frame
        if not 0 <= frame <= 8:
            raise ValueError("Frame out of range")
        self._frame = frame
        if show:
            self._register(self._CONFIG_BANK, self._FRAME_REGISTER, frame);

    def audio_sync(self, value=None):
        return self._register(self._CONFIG_BANK, self._AUDIOSYNC_REGISTER, value)

    def audio_play(self, sample_rate, audio_gain=0,
                   agc_enable=False, agc_fast=False):
        if sample_rate == 0:
            self._mode(self._PICTURE_MODE)
            return
        sample_rate //= 46
        if not 1 <= sample_rate <= 256:
            raise ValueError("Sample rate out of range")
        self._register(self._CONFIG_BANK, self._ADC_REGISTER, sample_rate % 256)
        audio_gain //= 3
        if not 0 <= audio_gain <= 7:
            raise ValueError("Audio gain out of range")
        self._register(self._CONFIG_BANK, self._GAIN_REGISTER,
                       bool(agc_enable) << 3 | bool(agc_fast) << 4 | audio_gain)
        self._mode(self._AUDIOPLAY_MODE)

    def blink(self, rate=None):
        if rate is None:
            return (self._register(self._CONFIG_BANK, self._BLINK_REGISTER) & 0x07) * 270
        elif rate == 0:
            self._register(self._CONFIG_BANK, self._BLINK_REGISTER, 0x00)
            return
        rate //= 270
        self._register(self._CONFIG_BANK, self._BLINK_REGISTER, rate & 0x07 | 0x08)

    def fill(self, color=None, blink=None, frame=None):
        if frame is None:
            frame = self._frame
        self._bank(frame)
        if color is not None:
            if not 0 <= color <= 255:
                raise ValueError("Color out of range")
            data = [color] * 24
            for row in range(6):
                self.i2c.write_i2c_block_data(self.address,
                                     self._COLOR_OFFSET + row * 24, data)
        if blink is not None:
            data = bool(blink) * 0xff
            for col in range(18):
                self._register(frame, self._BLINK_OFFSET + col, data)

    def _pixel_addr(self, x, y):
        return x + y * 16

    def pixel(self, x, y, color=None, blink=None, frame=None):
        if not 0 <= x <= self.width:
            print " X is too big"
            return
        if not 0 <= y <= self.height:
            print "Y is too big"
            return
        pixel = self._pixel_addr(x, y)
        if color is None and blink is None:
            return self._register(self._frame, pixel)
        if frame is None:
            frame = self._frame
        if color is not None:
            if not 0 <= color <= 255:
                raise ValueError("Color out of range")
            self._register(frame, self._COLOR_OFFSET + pixel, color)
        if blink is not None:
            addr, bit = divmod(pixel, 8)
            bits = self._register(frame, self._BLINK_OFFSET + addr)
            if blink:
                bits |= 1 << bit
            else:
                bits &= ~(1 << bit)
            self._register(frame, self._BLINK_OFFSET + addr, bits)


class CharlieWing(Matrix):
    width = 15
    height = 7

    def _pixel_addr(self, x, y):
        if x > 7:
            x = 15 - x
            y += 8
        else:
            y = 7 - y
        return x * 16 + y




