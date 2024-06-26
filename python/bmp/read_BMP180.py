#!/usr/bin/python
import smbus
import time
from ctypes import c_short

#  python read_bmp.py 


class ReadBmp180():
    DEVICE = 0x77 # Default device I2C address

    #bus = smbus.SMBus(0)  # Rev 1 Pi uses 0
    bus = smbus.SMBus(1) # Rev 2 Pi uses 1

    def convertToString(self,data):
      # Simple function to convert binary data into
      # a string
      return str((data[1] + (256 * data[0])) / 1.2)

    def getShort(self,data, index):
      # return two bytes from data as a signed 16-bit value
      return c_short((data[index]<< 8) + data[index + 1]).value

    def getUshort(self,data, index):
      # return two bytes from data as an unsigned 16-bit value
      return (data[index]<< 8) + data[index+1]

    def readBmp180Id(self,addr=DEVICE):
      # Register Address
      REG_ID     = 0xD0

      (chip_id, chip_version) = self.bus.read_i2c_block_data(addr, REG_ID, 2)
      return (chip_id, chip_version)

    def readBmp180(self,addr=DEVICE):
      # Register Addresses
      REG_CALIB  = 0xAA
      REG_MEAS   = 0xF4
      REG_MSB    = 0xF6
      REG_LSB    = 0xF7
      # Control Register Address
      CRV_TEMP   = 0x2E
      CRV_PRES   = 0x34
      # Oversample setting
      OVERSAMPLE = 3    # 0 - 3

      # Read calibration data
      # Read calibration data from EEPROM
      cal = self.bus.read_i2c_block_data(addr, REG_CALIB, 22)

      # Convert byte data to word values
      AC1 = self.getShort(cal, 0)
      AC2 = self.getShort(cal, 2)
      AC3 = self.getShort(cal, 4)
      AC4 = self.getUshort(cal, 6)
      AC5 = self.getUshort(cal, 8)
      AC6 = self.getUshort(cal, 10)
      B1  = self.getShort(cal, 12)
      B2  = self.getShort(cal, 14)
      MB  = self.getShort(cal, 16)
      MC  = self.getShort(cal, 18)
      MD  = self.getShort(cal, 20)

      # Read temperature
      self.bus.write_byte_data(addr, REG_MEAS, CRV_TEMP)
      time.sleep(0.005)
      (msb, lsb) = self.bus.read_i2c_block_data(addr, REG_MSB, 2)
      UT = (msb << 8) + lsb

      # Read pressure
      self.bus.write_byte_data(addr, REG_MEAS, CRV_PRES + (OVERSAMPLE << 6))
      time.sleep(0.04)
      (msb, lsb, xsb) = self.bus.read_i2c_block_data(addr, REG_MSB, 3)
      UP = ((msb << 16) + (lsb << 8) + xsb) >> (8 - OVERSAMPLE)

      # Refine temperature
      X1 = ((UT - AC6) * AC5) >> 15
      X2 = (MC << 11) / (X1 + MD)
      B5 = X1 + X2
      temperature = (B5 + 8) >> 4

      # Refine pressure
      B6  = B5 - 4000
      B62 = B6 * B6 >> 12
      X1  = (B2 * B62) >> 11
      X2  = AC2 * B6 >> 11
      X3  = X1 + X2
      B3  = (((AC1 * 4 + X3) << OVERSAMPLE) + 2) >> 2

      X1 = AC3 * B6 >> 13
      X2 = (B1 * B62) >> 16
      X3 = ((X1 + X2) + 2) >> 2
      B4 = (AC4 * (X3 + 32768)) >> 15
      B7 = (UP - B3) * (50000 >> OVERSAMPLE)

      P = (B7 * 2) / B4

      X1 = (P >> 8) * (P >> 8)
      X1 = (X1 * 3038) >> 16
      X2 = (-7357 * P) >> 16
      pressure = P + ((X1 + X2 + 3791) >> 4)

      return (temperature/10.0,pressure/ 100.0)

    def main(self):

      (chip_id, chip_version) = readBmp180Id()
      print "Chip ID     :", chip_id
      print "Version     :", chip_version

      print

      (temperature,pressure)=readBmp180()
      print "Temperature : ", temperature, "C"
      print "Pressure    : ", pressure, "mbar"



if __name__=="__main__":
    obj = ReadBmp180()
    obj.main()
