#!/usr/bin/python
import smbus
import time
from ctypes import c_short
import sys, getopt
import RPi.GPIO as GPIO

import base_i2c
import mcp_config
import mcp_pins

#python  ~/python/mcp23008_base.py -a 0x27 -b 1 -c [0x01,0x42,0xff] -r 1  -v

#python  ~/python/mcp23008_base.py -a 0x27 -b 1 -c [0x01,0x42,0xff] -d 1 -o ON   -v

#python  ~/python/mcp23008_base.py -a 0x27 -b 1  -v -k "{'foo':'bar'}"

# python  ~/python/mcp23008_base.py -a 0x27 -b 1  -v -k "{'pin1':{'dir':'in','pull':'down','default':'0','do_compare':'yes','int_ena':'yes'},'act':'high'}"

# Dictionary
# Major keys  
#  interrupt   act:hi/low
#
# pinX {     dir:in (IODIR) pull:up/down (GPPU)  default:0/1 (DEFVAL) do_compare:yes/no (INTCON) int_ena:yes/no (GPINTEN)   
    
   


# set pin 0 as output
#python  ~/python/mcp23008_base.py -a 0x27 -b 1 -c [0xfe] 
# drive pin 0 ON/OFF
#python  ~/python/mcp23008_base.py -a 0x27 -b 1  -d 0 -o ON   -v

# pin   0  output  clear bit 0 in IODIR
#       1  input  IODIR = 0x2  pullup  GPPU = 0x2


# Drive 0 high   
#    Get GPIO, set bit 0, update reg.


# interupt cleared by read of INTCAP or GPIO.

class MCP23800:
    
# MCP perspective
#  pin 0 LED
# pin 1 input  active high

# intr GPINTEN pin 1 monitored
#      DEFVAL left value 0 since pin 1 active high
#      INTCON set bit for pin 1.  Means, GPINTEN compared against DEFVAL, 
#              interupts when differ
#  So only interrutpts when pin 1 driven
#   IOCON bit 1 high for interrupt active high


# Pi-3 pins
    MCP_reset_pin = 25
    MCP_intr_pin = 26   # configed for active high
          
    
    def usage(self):
        print("options -v 'verbose',  -h 'help', -c config_chip [0x00,0x01], -b bus, -a address, -d drive-pin, -r read-pin ,-o ON/OFF, -x reset-chip")
     
    
    def __init__(self, bus = 1, address=0x27):
        self.bus_num = bus
        self.i2c = smbus.SMBus(self.bus_num) # Rev 2 Pi uses 1
        self.address = address
        self.pin = 0xff
        self.set_pin = False
        self.read_pin = False
        self.pin_on = False
        self.config_chip = False
        self.config_info = []
        self.do_reset = False
        self.keyed_data = {}
        self.reset()
        self.init()
                
    def init(self):
        GPIO.cleanup() # cleanup all GPIO that maybe left over from previous call
        GPIO.setmode(GPIO.BCM) # Broadcom pin-numbering scheme 
        GPIO.setup(self.MCP_reset_pin, GPIO.OUT, initial=1)
        GPIO.setup(self.MCP_intr_pin, GPIO.IN, pull_up_down=GPIO.PUD_DOWN)
              
    def sleep(self, value):
        time.sleep(value)
     
   
    def reset(self):
        self.sleep(True)
        time.sleep(0.00001)
        #utime.sleep_us(10)
        self.sleep(False)
    
    def process_opt_data(self, i2c_utils, bus,  address, config, pins, pin, key, value, verbose):
        if verbose:
            print "process_opt_data  pin%d, key %s  value %s"%(pin, key, value)
        if key.find('dir')==0:
             # read return a list, get the single entry [0]
            reg = i2c_utils.read_data_byte(bus,address, config._IODIR, 1, verbose)[0]
            if value.find('in')==0:
                reg = reg |(1<<pin)
            else:
                reg = reg & (~(1<<pin))
            i2c_utils.write_data(bus,address, config._IODIR, [reg],verbose)
        elif key.find('pull')==0:
             # read return a list, get the single entry [0]
            reg = i2c_utils.read_data_byte(bus,address, config._GPPU, 1, verbose)[0]
            if value.find('up')==0:
                reg = reg |(1<<pin)
            else:
                reg = reg & (~(1<<pin))
            i2c_utils.write_data(bus,address, config._GPPU, [reg],verbose)   
        elif key.find('default')==0:
             # read return a list, get the single entry [0]
            reg = i2c_utils.read_data_byte(bus,address, config._DEFVAL, 1, verbose)[0]
            if value.find('1')==0:
                reg = reg |(1<<pin)
            else:
                reg = reg & (~(1<<pin))
            i2c_utils.write_data(bus,address, config._DEFVAL, [reg],verbose)   
        elif key.find('do_compare')==0:
             # read return a list, get the single entry [0]
            reg = i2c_utils.read_data_byte(bus,address, config._INTCON, 1, verbose)[0]
            if value.find('yes')==0:
                reg = reg |(1<<pin)
            else:
                reg = reg & (~(1<<pin))
            i2c_utils.write_data(bus,address, config._INTCON, [reg],verbose)
        elif key.find('int_ena')==0:
             # read return a list, get the single entry [0]
            reg = i2c_utils.read_data_byte(bus,address, config._GPINTEN, 1, verbose)[0]
            if value.find('yes')==0:
                reg = reg |(1<<pin)
            else:
                reg = reg & (~(1<<pin))
            i2c_utils.write_data(bus,address, config._GPINTEN, [reg],verbose)         
                        
 
    
    def process_pin_data(self, i2c_utils, bus,  address, config, pins, pin, pin_data, verbose):
        if verbose:
            print "process_pin_data", pin_data
        opt_list = ['dir','pull','default','do_compare','int_ena']
        for i in range(len(opt_list)):
            if pin_data.has_key(opt_list[i]):
                value = pin_data[opt_list[i]]
                self.process_opt_data( i2c_utils, bus,  address, config, pins, pin, opt_list[i], value, verbose)
         
    
    def process_keyed_data(self,i2c_utils, bus,  address, config, pins, keyed_data, verbose):
        if verbose:
            print "keyed config data :", keyed_data
        #act:hi/low
        if keyed_data.has_key('act'):
            level = keyed_data['act']
            # read return a list, get the single entry [0]
            iocon_reg = i2c_utils.read_data_byte(bus,address, config._IOCON, 1, verbose)[0]
            if level.find('low')==0:
                iocon_reg = iocon_reg & (~2)
            elif level.find('high')==0:
                iocon_reg = iocon_reg | 2
            else:
                assert False, "process_keyed_data act, invalid level "  
            i2c_utils.write_data(bus,address, config._IOCON, [iocon_reg],verbose)
        
        pin_list = ['pin0','pin1','pin2','pin3','pin4','pin5','pin6','pin7']
        for i in range(len(pin_list)):
            if keyed_data.has_key(pin_list[i]):
                self.process_pin_data(i2c_utils, bus,  address, config, pins, i, keyed_data[pin_list[i]], verbose)
             
            
            
            
        
    def set_configuration(self, i2c_utils, bus,  address, config_data, config,pins,verbose):
        if verbose:
            print " set config ", config_data
        i2c_utils.write_data(bus,address, config._IODIR, config_data,verbose)
        
    def drive_pin(self, i2c_utils, bus, address, pin, pin_on, config,pins,verbose):
        if verbose:
            print "drive_pin: bus %d, address %x, pin %d, drive %s  " %(self.bus_num, self.address, self.pin, self.pin_on)
            print ' Pin register offset {:#x}'.format(config._GPIO)
        # get the regs and make sure the desired pin is configed as output.  Log error if not
        configed = i2c_utils.read_data_byte(bus,address, config._IODIR, 1, verbose)
        if (configed[0] &(1<<pin) ) != 0:
            assert False, "Pin %d not configured for output"  
        
        regs = i2c_utils.read_data_byte(bus,address, config._GPIO, 1, verbose)
        print regs
        if pin_on:
            regs = regs[0] | (1<< pin)
        else:
            regs = regs[0] & (~(1 << pin))
        # above statement retulted in an INT
        i2c_utils.write_data(bus,address, config._GPIO, [regs],verbose)
    
    def read_input(self, i2c_utils, bus, address, pin, config,pins,verbose):
        if verbose:
            print "read_pin: bus %d, address %x, pin %d  " %(self.bus_num, self.address, self.pin, )
            print ' Pin register offset {:#x}'.format(config._GPIO)
        # get the regs and make sure the desired pin is configed as input.  Log error if not
        configed = i2c_utils.read_data_byte(bus,address, config._IODIR, 1, verbose)
        if (configed[0] &(1<<pin) ) == 0:
            assert False, "Pin %d not configured for output"  
        regs = i2c_utils.read_data_byte(bus,address, config._GPIO, 1, verbose)
        if (regs[0] &(1<<pin)) == 0:
            print "Pin %d Low"%pin
        else:
            print "Pin %d high"%pin
            
    def dump_regs(self,i2c_utils, bus, device_addr, config,pins,verbose):
        regs = i2c_utils.read_data_byte(bus,device_addr, config._IODIR, config.chip_size, verbose)
        if verbose:
            print len(regs)
            print "Chip regs: " , regs
            for num in regs:
                print (format(num, '#04x'))
  
    def reset_chip(self,verbose):
        if verbose:
            print "reset_chip"
        GPIO.output(self.MCP_reset_pin,0)
        self.sleep(0.5)  
        GPIO.output(self.MCP_reset_pin,1)
        self.do_reset = False         

    def main(self,argv):
        for arg in sys.argv[1:]:
            pass # print arg
        try:
            opts, args = getopt.getopt(sys.argv[1:], "b:c:d:o:a:h:r:x:k:v", [])
           # opts, args = getopt.getopt(sys.argv[1:], "hb:v", ["help", "output="])
        except getopt.GetoptError as err:
            # print help information and exit:
            print(err) # will print something like "option b:bus,-a:address,-x:axis,-y:axis,-o:ON"
            self.usage()
            sys.exit(2)
        
        verbose = False
        for o, a in opts:
            if o == "-v":
                verbose = True
            elif o in ("-x"):
                if verbose:
                    print "reset chip"
                self.do_reset = True
            elif o in ("-k"):
                self.keyed_data = eval(a)
                if verbose:
                    print "keyed_data ",self.keyed_data  
            elif o in ("-b"):
                self.bus_num = int(a,10)
                if verbose:
                    print "bus_num",self.bus_num           
            elif o in ("-c"):
                self.config_chip = True
                number = ""
                for num in a:
                    if ((num.find('[') == 0) ):
                        continue
                    if ((num.find(',') == 0) or (num.find(']') == 0)):
                        self.config_info.extend([int(number[2:],16)])
                        number = ""
                    else:
                        number+= num
                if verbose:
                    print "Config chip with config_info ",self.config_info
            elif o in ("-d"):
                self.pin = int(a,10)
                self.set_pin = True
                if int(a,10) > 7:
                     assert False, "Invalid pin number, valid 0 - 7"
                if verbose:
                    print "drive pin number  ", self.pin 
            elif o in ("-r"):
                self.pin = int(a,10)
                self.read_pin = True
                if int(a,10) > 7:
                     assert False, "Invalid pin number, valid 0 - 7"
                if verbose:
                    print "read pin number  ", self.pin 
            elif o in ("-o"):
                data = {'ON':True, 'OFF':False}
                if a in data:
                    self.pin_on = data[a]
                    if verbose:
                        print "on/off  %s" %self.pin_on
                else:
                    print "-o requires ON or OFF:",a             
            elif o in ("-a"):
                if ((a.find('0x') == 0) or (a.find('0X') == 0)):
                    self.address = int(a[2:],16)
                    if verbose:
                        print "addr %#04x"%self.address
                else:
                    assert False, "-a (address)must be 0X format"            
            elif o in ("-h", "--help"):
                usage()
                sys.exit()
            else:
                assert False, "unhandled option"
     
        config = mcp_config.Mcp_config()
        pins = mcp_pins.Mcp_pins()
        i2c_utils = base_i2c.Base_i2c()
        self.dump_regs(i2c_utils, self.i2c,  self.address, config,pins,verbose)
        if self.do_reset:
            self.reset_chip(verbose)

        if len(self.keyed_data) > 0:
            self.process_keyed_data(i2c_utils, self.i2c,  self.address, config, pins, self.keyed_data, verbose)
            
        if self.config_chip:
            self.set_configuration(i2c_utils, self.i2c, self.address, self.config_info, config,pins,verbose)
            self.dump_regs(i2c_utils, self.i2c,  self.address, config,pins,verbose)
        
        if (self.set_pin):
            self.drive_pin(i2c_utils, self.i2c,  self.address, self.pin, self.pin_on, config,pins,verbose)     
        
        if (self.read_pin):
            self.read_input(i2c_utils, self.i2c,  self.address, self.pin, config,pins,verbose)     
        
        self.dump_regs(i2c_utils, self.i2c,  self.address, config,pins,verbose)
       


if __name__=="__main__":
    obj = MCP23800()
    obj.main(sys.argv[1:])
           
                   
                
   