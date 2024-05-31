#!/usr/bin/python

import time
import sys, getopt
import string
import datetime
from ctypes import c_short

LED_DEVICE = 0x74 # Default device I2C address
BMP_DEVICE = 0x77 # Default device I2C address

import is31fl3731_smbus
import display_led
import read_BMP180
import drive_LEDs


#  python display_temp.py  -b 1 -a 0X74  

#python display_temp.py  -b 1 -a 0X74  -c 1  -l 4

class DisplayTemp:
    def usage(self):
        print "python display_temp.py  -b 1 -a 0x74 -c <repeat_count,0 infinite>  -l <displays> -v "
     
    def process_bmp_data(self, bus_num, address, led_blink, loop_count, verbose):
        display_num = 42

        bmp = read_BMP180.ReadBmp180()
        (temp, pressure) = bmp.readBmp180()
        if verbose:
            print " Temp Celsius ", temp
            print " Presure " , pressure

        display_num = int((temp * 1.8) +32)
        if verbose:
            print "Display fahrenheit temp   " ,display_num
        display = is31fl3731_smbus.Matrix(bus_num, address)
        disp_worker = display_led.DisplayLed()
        pin_monitor = drive_LEDs.ControlLeds(verbose) 
        disp_worker.create_led_pattern(pin_monitor, display,display_num, led_blink, loop_count, False, verbose)

    def show_time(self, bus_num, address, led_blink, loop_count, verbose):
        # obtain a viewable version of the time and display the time
        now = datetime.datetime.now()
        if verbose:
            print " Hour %d,  Minute %d " %(now.hour, now.minute)
        time = "%02d:%02d"%(now.hour, now.minute)
        display = is31fl3731_smbus.Matrix(bus_num, address)
        disp_worker = display_led.DisplayLed()
        pin_monitor = drive_LEDs.ControlLeds(verbose) 
        disp_worker.create_led_pattern(pin_monitor, display, time, led_blink, loop_count, True, verbose)


    def main(self,argv):
        for arg in sys.argv[1:]:
            pass #print arg
        try:
            opts, args = getopt.getopt(sys.argv[1:], "h:b:a:c:l:v", [])
           # opts, args = getopt.getopt(sys.argv[1:], "hb:v", ["help", "output="])
        except getopt.GetoptError as err:
            # print help information and exit:
            print(err) # will print something like "option b:bus,-a:address,-x:axis,-y:axis,-o:ON"
            self.usage()
            sys.exit(2)
        address = 0xff
        bus_num = 0xff
        verbose = False
        loop_count = 1
        repeat_count = 0
        led_blink = 0
        for o, a in opts:
            if o == "-v":
                verbose = True
            elif o in ("-h", "--help"):
                usage()
                sys.exit()
            elif o in ("-b"):
                bus_num = int(a,10)
                if verbose:
                    print "bus_num",bus_num
            elif o in ("-l"):
                if int(a,10) > 7:
                     assert False, "Too many loops, MAX of 7"
                loop_count = int(a,10)
                if verbose:
                    print "loop count   ", loop_count            
            elif o in ("-a"):
                if ((a.find('0x') == 0) or (a.find('0X') == 0)):
                    address = int(a[2:],16)
                else:
                    assert False, "-a must be 0X format" 
            elif o in ("-c"):
                repeat_count = int(a,10)
            else:
                assert False, "unhandled option"

        if verbose:
            print "bus_num     :",bus_num
            print "Device address     :",address
            print "loop_count" , loop_count
   
        display = is31fl3731_smbus.Matrix(bus_num, address)
      
        if repeat_count > 0:
            for i in range(repeat_count):
                self.process_bmp_data(bus_num, address, led_blink, loop_count, verbose)
                time.sleep(2.0)
                self.show_time(bus_num, address, led_blink, loop_count, verbose)
                time.sleep(2.0)
            display.blink(0) 
            # clear the matrix
            for i in range(7):
                display.fill(1)
        else:
            while True:
                self.process_bmp_data(bus_num, address, led_blink, loop_count, verbose)
                time.sleep(2.0) 
                self.show_time(bus_num, address,led_blink, loop_count,  verbose)
                time.sleep(2.0)  
            display.blink(0) 
            # THE FOLLOWING NEEDS A CTL-C HANDLER
            for i in range(7):
                display.fill(1)
    

       





if __name__=="__main__":
   obj = DisplayTemp()
   obj.main(sys.argv[1:])


