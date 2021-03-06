/*
 * Monomanual Typeatron driver sketch, copyright 2014-2015 by Joshua Shinavier
 *
 * See: https://github.com/joshsh/extendo
 */

#include <Typeatron.h>

#include <ExtendoDevice.h>
#include <AnalogSampler.h>
#include <Droidspeak.h>
#include <ExtendOSC.h>
#include <Morse.h>
#include <RGBLED.h>
#include <OSCBundle.h>

Typeatron typeatron;

// TODO: tailor the bounce interval to the switch being used.
// This 2ms value is a conservative estimate based on an average over many kinds of switches.
// See "A Guide to Debouncing" by Jack G. Ganssle
unsigned int debounceMicros = 2000;

void setup() {
  typeatron.setup();

  typeatron.pingUntilConnected();
}

unsigned int lastKeyState = 0;

// basic key and laser loop
void loop() {
  unsigned long now = typeatron.beginLoop();

  typeatron.updateKeys();
  unsigned int keyState = typeatron.getKeyState();
  Mode mode = typeatron.getMode();
  if (keyState != lastKeyState) {
    if (LaserTrigger == mode) {
      if (keyState) {
        typeatron.laserOn();
        typeatron.sendLaserEvent();
        typeatron.setMode(LaserPointer);
      }
    } 
    else if (LaserPointer == mode) {
      if (!keyState) {
        typeatron.setMode(Normal);
        typeatron.laserOff();
      }
    } 
    else {       
      unsigned int before = micros();
      typeatron.sendKeyState();
      unsigned int after = micros();

      if (after - before < debounceMicros) {
        delayMicroseconds(debounceMicros - (after - before));
      }
    }
  } 

  lastKeyState = keyState;
}

