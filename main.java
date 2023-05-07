#
include < ThreeWire.h > #include < RtcDS1302.h > #include < EEPROM.h >

    #include < Wire.h > #include < hd44780.h > // main hd44780 header
    #include < hd44780ioClass / hd44780_I2Cexp.h > // i2c expander i/o class header

    #include < Keypad.h >

    #include < Servo.h >

    //  Motor setup
    Servo myservo;

const byte ROWS = 4;
const byte COLS = 4;

// Keypad Button mapping.
char HexaKeys[ROWS][COLS] = {
    {
        'N',
        'F',
        'S',
        'T'
    },
    {
        '5',
        '6',
        '7',
        '8'
    },
    {
        '*',
        '9',
        '0',
        'Y'
    },
    {
        '+',
        '^',
        '*',
        '-'
    }
};

/*
  PIN MAPPING
  Servo = 10;

  Black to Orange Cables.
  Black = Pin 2;
  Orange = Pin 8;

  RTC
  RST = 13;
  DAT = 12;
  CLK = 11;
*/

byte colPins[] = {
    5,
    4,
    3,
    2
};
byte rowPins[] = {
    6,
    7,
    8,
    9
};

Keypad customKeypad = Keypad(makeKeymap(HexaKeys), rowPins, colPins, ROWS, COLS);

// Lcd Object
hd44780_I2Cexp lcd;


// CONNECTIONS:
// DS1302 CLK/SCLK --> 6
// DS1302 DAT/IO --> 7
// DS1302 RST/CE --> 8
// DS1302 VCC --> 3.3v - 5v
// DS1302 GND --> GND
String PM_AM_TEMP = "";

int MODE = 2;
int disp = 0;

// Variables to contain time set by the user.
int on_OnHour = 6;
int on_OnMinute = 30;
String onPM_AM = "AM";

// Variables to contain time set by the user.
int off_OnHour = 6;
int off_OnMinute = 30;
String offPM_AM = "PM";


// Mode Reference
// 0 = SCHEDULED TIME
// 1 = 'Change Mode' for changing SCHEDULED TIME;
// 2 = 'Show Time Mode:'
int pos = 0;

// Default values
const byte HOUR_INDEX = 5;
const byte MIN_INDEX = 8;
const byte SEC_INDEX = 11;

char keyClicked;

// LCD setup
ThreeWire wire(12, 11, 13);

// Clock module setup
RtcDS1302 < ThreeWire > Rtc(wire);

// Setup function for instantiating objects to communicate with modules
void setup() {


    //  LCD initialization
    lcd.begin(16, 2);

    //  For logs
    Serial.begin(9600);

    //  RTC initialization
    RtcDateTime compiled = RtcDateTime(_DATE_, _TIME_);
    Serial.print("Invalid");
    Rtc.SetDateTime(compiled);


    Rtc.Begin();
    if (!Rtc.IsDateTimeValid()) {

        RtcDateTime compiled = RtcDateTime(_DATE_, _TIME_);
        Serial.print("Invalid");
        Rtc.SetDateTime(compiled);

    }

    //  Motor initialization
    myservo.attach(10);

}

//  Event loop
void loop() {

    //  Listen for keypad click.
    char keypad_ = customKeypad.getKey();

    //  Identify the MODE correlated with keypad click
    if (keypad_ != NO_KEY) {
        Serial.print(keyClicked);
        disp = 0;
        lcd.clear();
        if (keypad_ == 'N') {
            MODE = 1;
        } else if (keypad_ == 'T') {
            MODE = 2;
        } else if (keypad_ == 'S') {
            MODE = 0;
        } else if (keypad_ == 'F') {
            MODE = 3;
        }
    }

    //  Suspend the execution into MODE event loop
    if (MODE == 0) {
        printMode_0();
    } else if (MODE == 1) {
        printMode_1();
    } else if (MODE == 2) {
        printMode_2();
    } else if (MODE == 3) {
        printMode_3();
    }

}

//  Format display for time seconds
void formatSecDisplay(RtcDateTime sec) {
    lcd.setCursor(SEC_INDEX, 0);
    String s = String(sec.Second());
    if (sec.Second() < 10) {
        s = "0" + s;
    }
    lcd.print(s);
}

//  Rotates motor for turning on breaker
void onBreaker(RtcDateTime current) {
    int TEMPORARY_HOUR = 0;
    int currentHour = current.Hour();

    if (onPM_AM.equalsIgnoreCase("PM")) {
        TEMPORARY_HOUR = on_OnHour + 12;
    } else if (onPM_AM.equalsIgnoreCase("AM")) {
        TEMPORARY_HOUR = on_OnHour;
    }
    Serial.println(TEMPORARY_HOUR);
    Serial.println(currentHour);
    if (currentHour == TEMPORARY_HOUR && current.Minute() == on_OnMinute) {
        myservo.write(0);
    }
}

//  Rotates motor for turning off breaker
void offBreaker(RtcDateTime current) {
    int TEMPORARY_HOUR = 0;

    int currentHour = current.Hour();
    if (offPM_AM.equalsIgnoreCase("PM")) {
        TEMPORARY_HOUR = off_OnHour + 12;
    } else if (offPM_AM.equalsIgnoreCase("AM")) {
        TEMPORARY_HOUR = off_OnHour;
    }

    if (currentHour == TEMPORARY_HOUR && current.Minute() == off_OnMinute) {
        myservo.write(180);
    }
}

//  Format time minutes
void formatMinDisplay(RtcDateTime minutes) {
    lcd.setCursor(MIN_INDEX, 0);
    String m = String(minutes.Minute());
    if (minutes.Minute() < 10) {
        m = "0" + m;
    }
    lcd.print(m);
}

//  Format time hour
void formatHourDisplay(RtcDateTime hour) {
    lcd.setCursor(HOUR_INDEX, 0);
    long Hours = hour.Hour();
    if (Hours > 12) {
        Hours = Hours - 12;
    }

    String hr = String(Hours);
    if (Hours < 10) {
        hr = "0" + hr;
    }
    lcd.print(hr);
}

//  Prompts the user to enter hour. Has its own event loop
int getHour() {
    lcd.setCursor(0, 1);
    lcd.print("HOUR:");
    int hour_temp = 0;
    int onPM_AM_temp = 0;
    lcd.setCursor(8, 1);
    lcd.print("AM");
    lcd.setCursor(5, 1);
    lcd.print("00");
    while (true) {
        char clicked = customKeypad.getKey();
        if (clicked != NO_KEY) {
            if (clicked == 'Y') {
                lcd.setCursor(0, 1);
                lcd.print("                ");
                if (onPM_AM_temp == 0) {
                    PM_AM_TEMP = "AM";
                } else if (onPM_AM_temp == 1) {
                    PM_AM_TEMP = "PM";
                }
                break;
            } else if (clicked == '+') {
                if (hour_temp != 12) {
                    hour_temp++;
                }
            } else if (clicked == '-') {
                if (hour_temp != 0) {
                    hour_temp--;
                }
            } else if (clicked == '^') {
                onPM_AM_temp = onPM_AM_temp == 0 ? 1 : 0;
            }
            lcd.setCursor(8, 1);
            if (onPM_AM_temp == 0) {
                lcd.print("AM");
            } else {
                lcd.print("PM");
            }
            lcd.setCursor(5, 1);
            lcd.print("  ");
            lcd.setCursor(5, 1);
            lcd.print(hour_temp);
        }
    }
    return hour_temp;
}

//  Prompts the user to enter minute. Has its own event loop
int getMinute() {
    int minute_temp = 0;
    lcd.setCursor(0, 1);
    lcd.print("Minute:");
    lcd.setCursor(7, 1);
    lcd.print("00");
    while (true) {
        char clicked = customKeypad.getKey();
        if (clicked != NO_KEY) {
            if (clicked == 'Y') {
                lcd.clear();
                MODE = 0;
                break;
            } else if (clicked == '+') {
                if (minute_temp != 60) {
                    minute_temp++;
                }
            } else if (clicked == '-') {
                if (minute_temp != 0) {
                    minute_temp--;
                }
            }
            lcd.setCursor(7, 1);
            lcd.print("  ");
            lcd.setCursor(7, 1);
            lcd.print(minute_temp);
        }
    }
    return minute_temp;
}

//  Print the details of a MODE 2. MODE 2 Shows current time.
void printMode_2() {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("TIME: ");
    lcd.setCursor(5, 0);
    lcd.print("00:00:00");
    while (true) {
        RtcDateTime current = Rtc.GetDateTime();
        onBreaker(current);
        offBreaker(current);
        RtcDateTime currTime = Rtc.GetDateTime();
        formatSecDisplay(currTime);
        formatMinDisplay(currTime);
        formatHourDisplay(currTime);
        lcd.setCursor(0, 1);
        String hrhr = String(currTime.Hour());
        lcd.print(hrhr);
        char clicked = customKeypad.getKey();
        if (clicked != NO_KEY) {
            if (clicked == 'N') {
                MODE = 1;
            } else if (clicked == 'T') {
                MODE = 2;
            } else if (clicked == 'S') {
                MODE = 0;
            } else if (clicked == 'F') {
                MODE = 3;
            }
            break;
        }
    }
}


//  Print the details of a MODE 0. Prints when the motor will turn the breaker on and off
void printMode_0() {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("ON: ");
    lcd.setCursor(0, 1);
    lcd.print("OFF: ");
    formatSchedDisplay();
    while (true) {
        RtcDateTime current = Rtc.GetDateTime();
        onBreaker(current);
        offBreaker(current);
        char clicked = customKeypad.getKey();
        if (clicked != NO_KEY) {
            Serial.println(clicked);

            if (clicked == 'N') {
                MODE = 1;
            } else if (clicked == 'T') {
                MODE = 2;
            } else if (clicked == 'S') {
                MODE = 0;
            } else if (clicked == 'F') {
                MODE = 3;
            }
            break;
        }
    }
}



//  Print the details of a MODE 1. Mode 1 is where user enters the time where the motor will turn the breaker ON
void printMode_1() {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Open On:");

    on_OnHour = getHour();
    on_OnMinute = getMinute();
    onPM_AM = PM_AM_TEMP;
    PM_AM_TEMP = "";
    MODE = 0;
}

//  Print the details of a MODE 3. Mode 3 is where user enters the time where the motor will turn the breaker OFF
void printMode_3() {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Close on");
    off_OnHour = getHour();
    off_OnMinute = getMinute();
    offPM_AM = PM_AM_TEMP;
    PM_AM_TEMP = "";
    MODE = 0;
}


//  Formats the data displayed on the LCD
void formatSchedDisplay() {
    lcd.setCursor(3, 0);
    lcd.print("00:00");
    String HOUR = String(on_OnHour);
    if (on_OnHour < 10) {
        HOUR = "0" + HOUR;
    }
    lcd.setCursor(3, 0);
    lcd.print(HOUR);


    String MINUTE = String(on_OnMinute);
    if (on_OnMinute < 10) {
        MINUTE = "0" + MINUTE;
    }
    lcd.setCursor(6, 0);
    lcd.print(MINUTE);

    lcd.setCursor(8, 0);
    lcd.print(onPM_AM);
    //

    lcd.setCursor(4, 1);
    lcd.print("00:00");
    HOUR = String(off_OnHour);
    if (off_OnHour < 10) {
        HOUR = "0" + HOUR;
    }
    lcd.setCursor(4, 1);
    lcd.print(HOUR);


    MINUTE = String(off_OnMinute);
    if (off_OnMinute < 10) {
        MINUTE = "0" + MINUTE;
    }
    lcd.setCursor(7, 1);
    lcd.print(MINUTE);

    lcd.setCursor(9, 1);
    lcd.print(offPM_AM);
}