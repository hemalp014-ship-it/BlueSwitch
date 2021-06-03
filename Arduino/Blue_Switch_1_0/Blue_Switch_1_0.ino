#define DEBUG

//Decleration of Pins for Devices
int Device_1_pin = 12;   //First Pin
int Device_2_pin = 11;   //Second Pin
int Device_3_pin = 10;   //Third Pin
int Device_4_pin = 9;    //Fourth Pin

//Device on which work is going on, -1 means null 
int Device_Curr = -1;

//Status of Devices
bool Device_1_on = false;
bool Device_2_on = false;
bool Device_3_on = false;
bool Device_4_on = false;

//Code specifier
const char Acquire  = 'a';   // Start Inquiry, usually started by User end application to acquire the state of Devices connected to our board
const char device   = 'd';
const char on  = 'o';   //Device -> On
const char off = 'f';   //Device -> Off

// Run once
void setup() {

  //Defining output pins
  pinMode(Device_1_pin, OUTPUT);
  pinMode(Device_2_pin, OUTPUT);
  pinMode(Device_3_pin, OUTPUT);
  pinMode(Device_4_pin, OUTPUT);

  //For testing first pin is set to high
  //Device_1_on = true;
  //digitalWrite(Device_1_pin, HIGH);

  //Begins Serial work at bit-rate 115200 bits/sec
  Serial.begin(115200);
}

void AcquireState(){
    Serial.write(device);
    Serial.write((char) 1);
    Serial.write(Device_1_on ? on : off);
    Serial.write((char) 0);
    Serial.write(device);
    Serial.write((char) 2);
    Serial.write(Device_2_on ? on : off);
    Serial.write((char) 0);    
    Serial.write(device);
    Serial.write((char) 3);
    Serial.write(Device_3_on ? on : off);
    Serial.write((char) 0);    
    Serial.write(device);
    Serial.write((char) 4);
    Serial.write(Device_4_on ? on : off);
    Serial.write((char) 0);   
    Serial.write(Acquire);
    Serial.write(off);
    Serial.write((char) 0);
    Serial.write((char) '\r');
    Serial.write((char) '\n');

    Serial.flush();
}
  
// Run repeatedly
void loop() {
    while(Serial.available() > 0){
        char data[100];
        int i = Serial.readBytesUntil('\0', data, 100);
        //data[i] = '\0';

        if(data[0] == 'a'){
            if(data[1] == on)
                AcquireState();
        } else if(data[0] == 'd'){
            bool s = data[2] == on;
            int state = s ? HIGH : LOW;
            switch(data[1]){
                case (char) 1:
                    Device_Curr = Device_1_pin; 
                    Device_1_on = s;
                    break;
                case (char) 2:
                    Device_Curr = Device_2_pin; 
                    Device_2_on = s;
                    break;
                case (char) 3:
                    Device_Curr = Device_3_pin; 
                    Device_3_on = s;
                    break;
                case (char) 4:
                    Device_Curr = Device_4_pin; 
                    Device_4_on = s;
                    break;
            }
            if(Device_Curr > -1){
                digitalWrite(Device_Curr, state);
            }
        }
    }
}
