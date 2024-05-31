#!/usr/bin/python
###import smbus
import time
from ctypes import c_short
import sys, getopt



# python  ~/python/mcp23008_base.py -a 0x27 -b 1 -c [0x01,0x02] -v

class Base_i2c:
 

    def write_data(self, bus,device_addr, offset, data,verbose):
        if verbose:
            print "write_data to device addr  %#04x, offset %#04x" %(device_addr, offset) 
            print "data  " , data
        bus.write_i2c_block_data(device_addr, offset, data) 
            
    def read_data_byte(self, bus,device_addr, offset,num_bytes, verbose):
        if verbose:
            print " read_data_byte from device_addr %x " %( device_addr)
            print " device offset %x, number of bytes %d "%(offset,num_bytes)
        return(bus.read_i2c_block_data(device_addr, offset, num_bytes))
 