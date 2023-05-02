#include <SoftwareSerial.h>

#define IR_PIN 7
#define LED_PIN 13
#define BUZZER_PIN 11
#define BUZZER_FREQ 10000

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
        int option = BTSerial.readStringUntil(':').substring(2, 3).toInt();
        int value = BTSerial.readString().toInt();
        Serial.print("Option:");
        Serial.println(option);
        if (option == 0) // BLINK_DELAY
        {
            BLINK_DELAY = value;
        }
        else if (option == 1) // RESET_TIME
        {
            RESET_TIME = value;
        }
        makeNoise(300);
        Serial.print("New blink delay:");
        Serial.println(BLINK_DELAY);
        Serial.print("New reset time:");
        Serial.println(RESET_TIME);
    }

    int ir_new = digitalRead(7);

    if (ir_new == 0) // if eye is closed
    {
        if (ir_old == 1) // if eye was previously closed
        {
            is_reset_send = false;
            blink_start_time = millis();
        }
    }
    else // if eye is open
    {
        if (ir_old == 0) // if eye was previously closed
        {
            blink_end_time = millis();
            blink_duration = blink_end_time - blink_start_time;
            unblink_start_time = blink_end_time;

            if (blink_duration <= BLINK_DELAY && blink_duration > 100) // if short blink
            {
                BTSerial.write((uint8_t)0);
                makeNoise(100);
            }
            else if (blink_duration > BLINK_DELAY) // if long blink
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
    ir_old = ir_new;
}

void makeNoise(int ms)
{
    tone(BUZZER_PIN, BUZZER_FREQ);
    delay(ms);
    noTone(BUZZER_PIN);
}
