#!/usr/bin/python

import time
import sys, getopt
import string

from ctypes import c_short

DEVICE = 0x74 # Default device I2C address

import is31fl3731_smbus
#import is31fl3731_smbus_test
import drive_LEDs


#python display_led.py  -b 1 -a 0X74  -o ON -n 10 -w STEADY

class DisplayLed():
        
    def usage(self):
        print("option f:func,b:bus,-a:address,-n number,-x:axis,-y:axis,-o:ON")
        print "python display_led.py  -b 1 -a 0X74  -o ON -t 12:34  -w STEADY -l 1"
        print "python display_led.py  -b 1 -a 0x74 -x 0 -y 0 -o ON "
        print "python display_led.py  -b 1 -a 0x74 -n 1 -w BLINK or STEADY"

# pixels for 0 1 2 3 4 5 6 7 8 9 :
# First element is the width in pixels of the displayed number
    pixels = [ [ [3],[[0,0],[1,0],[2,0],[0,1],[2,1],[0,2],[2,2],[0,3],[2,3],[0,4],[2,4],[0,5],[2,5],[0,6],[2,6],[2,6],[1,6]] ],
               [ [3],[[0,1],[1,0],[1,1],[1,2],[1,3],[1,4],[1,5],[1,6],[0,6],[2,6]] ],
               [ [4],[[0,1],[0,2],[1,0],[2,0],[3,1],[3,2],[3,3],[2,4],[1,5],[0,6],[1,6],[2,6],[3,6]] ] ,
               [ [4],[[0,1],[1,0],[2,0],[3,1],[3,2],[2,3],[3,4],[3,5],[0,5],[1,6],[2,6]] ],
               [ [4],[[0,0],[0,1],[0,2],[0,3],[1,3],[2,3],[3,0],[3,1],[3,2],[3,3],[3,4],[3,5],[3,6]] ],
               [ [4],[[0,0],[0,1],[0,2],[0,6],[1,0],[2,0],[3,0],[1,2],[2,3],[3,4],[2,5],[1,6],[0,6]] ],
               [ [4],[[1,0],[0,1],[0,2],[0,3],[0,4],[0,5],[1,3],[2,3],[3,4],[3,5],[2,6],[1,6]] ],
               [ [4],[[0,0],[0,1],[1,0],[2,0],[3,0],[3,1],[3,2],[3,3],[3,4],[3,5],[3,6]] ],
               [ [4],[[0,1],[0,2],[1,0],[2,0],[3,1],[3,2],[1,3],[2,3],[0,4],[0,5],[1,6],[2,6],[3,4],[3,5]] ],
               [ [4],[[0,1],[0,2],[1,0],[2,0],[3,1],[3,2],[1,3],[2,3],[3,3],[3,4],[3,5],[3,6]] ],
               [ [1],[[0,2],[0,4]] ]
              ]
            
    space_between_symbols = 0

    def write_num(self,number, digit_count, plus_x,display_dev, led_blink):
        the_list = self.pixels[number]
        # plus_x used to place items across matrix
           
        for entry in the_list[1]:
            display_dev.pixel(plus_x+entry[0],entry[1],10,led_blink, 0)
            #  Use digit_count and each digit is on a new frams,
            # or put them all on the same frame if last parm '0'
        next_displace = plus_x + (the_list[0][0] + self.space_between_symbols)
        return next_displace
        

    def create_led_pattern(self,pin_monitor,display_dev, display_num, led_blink, loop_count, time_mode, verbose):
        if verbose:
            print "create_led_pattern:display in leds : time_mode %s, display_num %s "%(time_mode, display_num)
        if time_mode:
            ascii_num = display_num
        else:
            ascii_num = "%d"%display_num
        offset = 0 
        placement = 0  # aded to x axis value in setting the pixels
        # special format when displaying time
        if time_mode: # we expect format HH:MM in string format
            num = int(display_num[0],10)
            placement = self.write_num(num, offset, placement, display_dev, led_blink)
            offset += 1
            #
            num = int(display_num[1],10)
            placement = self.write_num(num, offset, placement, display_dev, led_blink)
            offset += 1
            #
            num = 10  # pixel array for base 10, has an eleventh entry for the :
            display_dev.blink(540) 
            placement =self.write_num(num, offset, placement, display_dev, 1   )#led_blink) # force blinking
            offset += 1
            #
            num = int(display_num[3],10)
            placement = self.write_num(num, offset, placement, display_dev, led_blink)
            offset += 1
            #
            num = int(display_num[4],10)
            placement = self.write_num(num, offset, placement, display_dev, led_blink)
            offset += 1
            
        else:
            for i in range(len(ascii_num)):
                num = int(ascii_num[i],10)
                placement = self.write_num(num, offset,placement, display_dev, led_blink)
                offset += 1
            
            
            
        display_dev.autoplay(delay =  704, loops=loop_count, frames=0) #offset
        # wait for the autoplay to complete
        completed = pin_monitor.monitor_intr(verbose)
         
        if verbose:
            print "monitor_intr returned ", completed

    def main(self,argv):
        for arg in sys.argv[1:]:
            pass #print arg
        try:
            opts, args = getopt.getopt(sys.argv[1:], "h:b:a:n:x:y:o:w:l:t:v", [])
           # opts, args = getopt.getopt(sys.argv[1:], "hb:v", ["help", "output="])
        except getopt.GetoptError as err:
            # print help information and exit:
            print(err) # will print something like "option b:bus,-a:address,-x:axis,-y:axis,-o:ON"
            self.usage()
            sys.exit(2)
        address = 0xff
        bus_num = 0xff
        x_axis = 0xff
        y_axis = 0xff
        led_on = False
        led_blink = 0
        verbose = False
        display_num = -1
        loop_count = 0
        time_mode = False
        for o, a in opts:
            if o == "-v":
                verbose = True
            elif o in ("-h", "--help"):
                usage()
                sys.exit()
            elif o in ("-f"):
                data = {'fill':1}
                if (a in data):
                    func_num = data[a]
                else:
                    print "Function name not found",a
            elif o in ("-b"):
                bus_num = int(a,10)
                if verbose:
                    print "bus_num",bus_num
            elif o in ("-a"):
                if ((a.find('0x') == 0) or (a.find('0X') == 0)):
                    address = int(a[2:],16)
                else:
                    assert False, "-a must be 0X format"
            elif o in ("-n"):
                num_digits = len(a)
                if num_digits > 3:
                     assert False, "Too many digits to display, MAX of 3"
                display_num = int(a,10)
                if verbose:
                    print "display_number   ", display_num
            elif o in ("-t"):
                num_digits = len(a)
                if num_digits != 5:
                     assert False, "Wrong number digits: use HH:MM"
                display_num = a
                time_mode = True
                if verbose:
                    print "display_time   ", display_time
            elif o in ("-l"):
                if int(a,10) > 7:
                     assert False, "Too many loops, MAX of 7"
                loop_count = int(a,10)
                if verbose:
                    print "loop count   ", loop_count
            elif o in ("-x"):
                x_axis = int(a,10)
                if verbose:
                    print "x_axis",x_axis
            elif o in ("-y"):
                y_axis = int(a,10)
                if verbose:
                    print "y_axis",y_axis
            elif o in ("-o"):
                data = {'ON':True, 'OFF':False}
                if a in data:
                    led_on = data[a]
                else:
                    print "-o requires ON or OFF:",a
            elif o in ("-w"):
                data = {'BLINK':1,'STEADY':0}
                if a in data:
                    led_blink = data[a]
                else:
                    print "-o requires ON or OFF:",a      
            else:
                assert False, "unhandled option"

        if verbose:
            print "  bus_num     :",bus_num
            print "  Device address     :",address
            print "  Display number  :" ,display_num
            print "  Time mode  :" ,time_mode
            print "  x_axis    :",x_axis
            print "  y_axis    :",y_axis
            print "  led_on :",led_on
            print "  led_blink  :", led_blink

       # display = is31fl3731_smbus_test.Matrix(bus_num, address)
        display = is31fl3731_smbus.Matrix(bus_num, address)
        pin_monitor = drive_LEDs.ControlLeds(verbose) 

        if display_num != -1:
            self.create_led_pattern(pin_monitor,display,display_num, led_blink, loop_count, time_mode, verbose)
        else:
            display.fill(10)


# end of class


if __name__=="__main__":
    obj = DisplayLed()
    obj.main(sys.argv[1:])



