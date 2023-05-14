#include <SoftwareSerial.h>

#define IR_PIN 3
#define LED_PIN 13
#define BUZZER_PIN 10
#define BUZZER_FREQ 1000

int BLINK_DELAY = 600;
int RESET_TIME = 2000;

long int blink_duration;
long int blink_start_time, blink_end_time;
long int unblink_start_time;
int ir_new, ir_old;
bool is_reset_send = false;
bool data_send = false;
SoftwareSerial BTSerial(8, 9); // Rx,Tx

void setup()
{
    ir_old = 1;
    unblink_start_time = millis();
    pinMode(IR_PIN, INPUT);
    pinMode(LED_PIN, OUTPUT);
    Serial.begin(9600);
    BTSerial.begin(9600);
}

void loop()
{
  if (BTSerial.available() > 0) // Check if some data recieved from application layer
    {
      String message=BTSerial.readString();
      int option=message.charAt(2)-'0';
      int value=message.substring(4,message.length()).toInt();
      if (option == 0) // BLINK_DELAY
      {
          BLINK_DELAY = value;
          makeNoise(300);
      }
      else if (option == 1) // RESET_TIME
      {
          RESET_TIME = value;
          makeNoise(300);
      }
      else if(option==2) //To make confirmation beep by patient
      {
        makeNoise(100);
        delay(100);
        makeNoise(100);
      }
      Serial.print("New blink delay:");
      Serial.println(BLINK_DELAY);
      Serial.print("New reset time:");
      Serial.println(RESET_TIME);
    }

  int ir_new=digitalRead(IR_PIN);

  if(ir_new==LOW)//if eye is closed
  {
    if (ir_old == HIGH) // if eye was previously open
        {
            is_reset_send = false;
            blink_start_time = millis();
        }
  }
  else //if eye is open
  {
    if(ir_old==LOW)// if previously eye was closed
    {
      blink_end_time=millis();
      blink_duration=blink_end_time-blink_start_time;
      unblink_start_time=blink_end_time;

      if(blink_duration<=BLINK_DELAY && blink_duration>100) //if short blink
      {
        BTSerial.write((uint8_t)0);
        makeNoise(100);
      }
      else if(blink_duration>BLINK_DELAY)
      {
        BTSerial.write((uint8_t)1);
        makeNoise(100);
      }
    }
    if ((millis() - unblink_start_time >= RESET_TIME) && !is_reset_send) // if eye is idle for RESET_TIME sec, then evaluate the message
    {
      BTSerial.write((uint8_t)2);
      is_reset_send = true;
      makeNoise(300);
    }
  }
  
  ir_old=ir_new;
}

void makeNoise(int ms)
{
    tone(BUZZER_PIN, BUZZER_FREQ);
    delay(ms);
    noTone(BUZZER_PIN);
}

