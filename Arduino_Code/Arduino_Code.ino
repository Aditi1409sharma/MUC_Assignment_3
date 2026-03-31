#define TRIG_PIN 9
#define ECHO_PIN 10
#define LDR_PIN A0

void setup() {
  Serial.begin(9600);
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
}

void loop() {
  // 1. Measure Distance (Ultrasonic)
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  
  long duration = pulseIn(ECHO_PIN, HIGH);
  float distance = duration * 0.034 / 2;

  // 2. Measure Light (LDR)
  int lightValue = analogRead(LDR_PIN);

  // 3. Send Data to Android via HC-05
  Serial.print(distance);
  Serial.print(",");
  Serial.println(lightValue);

  delay(200); // Send data 5 times per second
}