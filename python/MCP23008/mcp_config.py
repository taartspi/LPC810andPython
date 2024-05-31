#!/usr/bin/python
###import smbus
import time
from ctypes import c_short
import sys, getopt


class Mcp_config:
 
    # Register Address
    _IODIR      = 0x00
    _IPOL       = 0x01
    _GPINTEN    = 0x02
    _DEFVAL     = 0x03
    _INTCON     = 0x04
    _IOCON      = 0x05
    _GPPU       = 0x06
    _INTF       = 0x07
    _INTCAP     = 0x08
    _GPIO       = 0x09
    _OLAT       = 0x0A
  
    chip_size   = 0x0B 
    pass
    
    