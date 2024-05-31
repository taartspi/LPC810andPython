#!/usr/bin/python

import RPi.GPIO as GPIO
import time
import sys, getopt
import string


class ControlLeds():
    
    def usage(self):
        print("option -v 'verbose'  -h 'help'")
        
    # Pin Definitons:   Uses GPIO number(broadcom values)
    ledPinGrn = 23 
    ledPinRed = 24 
    pinIntr = 22 
     
    dc = 95 # duty cycle (0-100) for PWM pin

    def __init__(self, verbose=False):
        # Pin Setup:
        GPIO.cleanup() # cleanup all GPIO that maybe left over from previous call
        GPIO.setmode(GPIO.BCM) # Broadcom pin-numbering scheme
        GPIO.setup(self.ledPinRed, GPIO.OUT) # LED pin set as output
        GPIO.setup(self.ledPinGrn, GPIO.OUT) # PWM pin set as output
        GPIO.setup(self.pinIntr, GPIO.IN, pull_up_down=GPIO.PUD_UP)
        self.init(verbose)
    
    
    def init(self, verbose):
        pass
        
        
    def toggle_led(self, active, verbose):
        if verbose:
            pass
            #print " togle_active ", verbose
        if active:
            GPIO.output(self.ledPinRed, GPIO.LOW)
            GPIO.output(self.ledPinGrn, GPIO.HIGH)
        else:
            GPIO.output(self.ledPinRed, GPIO.HIGH)
            GPIO.output(self.ledPinGrn, GPIO.LOW)
        
    def monitor_intr(self, verbose):
        is_done = False 
        while True:
           # time.sleep(0.075)
            time.sleep(0.003)
            if (GPIO.input(self.pinIntr)):
                if verbose:
                    pass
                    #print("Pin High ")
                self.toggle_led(True, verbose)
            else:
                if verbose:
                    print("Pin Low")
                self.toggle_led(False, verbose)
                is_done = True
                break
        return is_done
                
                
            
    def main(self,argv):
        for arg in sys.argv[1:]:
            pass #print arg
        try:
            opts, args = getopt.getopt(sys.argv[1:], "h:v", [])
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
            elif o in ("-h", "--help"):
                usage()
                sys.exit()
            else:
                assert False, "unhandled option"
              
        self.monitor_intr(verbose) 
        # need ctl-c handler to call this function
        GPIO.cleanup() # cleanup all GPIO       
     


if __name__=="__main__":
    obj = ControlLeds()
    obj.main(sys.argv[1:])
           
                   
                
   