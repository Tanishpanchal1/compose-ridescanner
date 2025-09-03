# automation_server.py
from flask import Flask, request, jsonify
from appium import webdriver
from appium.options.android import UiAutomator2Options
import time
import logging
import json
from concurrent.futures import ThreadPoolExecutor
import threading

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

class MobileAppAutomator:
    def __init__(self):
        self.driver_lock = threading.Lock()
        self.drivers = {}
        
    def get_driver(self, app_package):
        with self.driver_lock:
            if app_package not in self.drivers:
                options = UiAutomator2Options()
                options.platform_name = "Android"
                options.device_name = "emulator-5554"
                options.app_package = app_package
                options.no_reset = True
                options.new_command_timeout = 300
                
                try:
                    driver = webdriver.Remote("http://localhost:4723/wd/hub", options=options)
                    self.drivers[app_package] = driver
                    logging.info(f"Created driver for {app_package}")
                except Exception as e:
                    logging.error(f"Failed to create driver for {app_package}: {e}")
                    return None
                    
            return self.drivers[app_package]
    
    def extract_uber_data(self, pickup_lat, pickup_lng, dropoff_lat, dropoff_lng):
        driver = self.get_driver("com.ubercab")
        if not driver:
            return []
            
        try:
            # Navigate to ride estimation screen
            self._navigate_to_ride_screen(driver)
            
            # Set pickup location
            self._set_pickup_location(driver, pickup_lat, pickup_lng)
            
            # Set dropoff location  
            self._set_dropoff_location(driver, dropoff_lat, dropoff_lng)
            
            # Wait for estimates to load
            time.sleep(5)
            
            # Extract ride options
            rides = self._extract_ride_estimates(driver)
            
            return rides
            
        except Exception as e:
            logging.error(f"Uber extraction error: {e}")
            return []
    
    def _navigate_to_ride_screen(self, driver):
        try:
            # Close any popups/promotions first
            try:
                close_buttons = driver.find_elements("xpath", "//android.widget.Button[@text='Close']")
                for button in close_buttons:
                    button.click()
                    time.sleep(1)
            except:
                pass
                
            # Look for "Where to?" field
            where_to_field = driver.find_element("xpath", 
                "//android.widget.EditText[contains(@text, 'Where to')]")
            where_to_field.click()
            time.sleep(2)
            
        except Exception as e:
            logging.error(f"Navigation error: {e}")
            
    def _set_pickup_location(self, driver, lat, lng):
        try:
            # Try to use current location button first
            current_location_btn = driver.find_element("xpath", 
                "//android.widget.Button[contains(@text, 'Current')]")
            current_location_btn.click()
            time.sleep(2)
        except:
            # Manual coordinate entry if needed
            self._enter_coordinates(driver, lat, lng, is_pickup=True)
    
    def _set_dropoff_location(self, driver, lat, lng):
        try:
            # Find destination input field
            dest_field = driver.find_element("xpath", 
                "//android.widget.EditText[contains(@hint, 'destination')]")
            dest_field.click()
            dest_field.clear()
            
            # You might need to convert coordinates to address
            # For now, using coordinates directly
            dest_field.send_keys(f"{lat},{lng}")
            time.sleep(3)
            
            # Select first suggestion
            try:
                first_suggestion = driver.find_element("xpath", 
                    "//android.widget.ListView//android.widget.TextView[1]")
                first_suggestion.click()
                time.sleep(3)
            except:
                # Press enter if no suggestions
                driver.press_keycode(66)  # Enter key
                time.sleep(3)
                
        except Exception as e:
            logging.error(f"Dropoff location error: {e}")
    
    def _extract_ride_estimates(self, driver):
        rides = []
        try:
            # Wait for ride options to appear
            time.sleep(5)
            
            # Find all ride option cards
            ride_cards = driver.find_elements("xpath", 
                "//android.widget.LinearLayout[contains(@resource-id, 'ride_option')]")
            
            for card in ride_cards:
                try:
                    # Extract ride type
                    ride_type = card.find_element("xpath", 
                        ".//android.widget.TextView[contains(@resource-id, 'vehicle_name')]").text
                    
                    # Extract price
                    price_text = card.find_element("xpath", 
                        ".//android.widget.TextView[contains(@resource-id, 'price')]").text
                    price = self._parse_price(price_text)
                    
                    # Extract ETA
                    eta_text = card.find_element("xpath", 
                        ".//android.widget.TextView[contains(@resource-id, 'eta')]").text
                    eta_seconds = self._parse_eta(eta_text)
                    
                    rides.append({
                        "vehicle_type": ride_type,
                        "price_estimate": price,
                        "eta_seconds": eta_seconds,
                        "service": "uber"
                    })
                    
                except Exception as e:
                    logging.error(f"Error extracting ride card: {e}")
                    continue
                    
        except Exception as e:
            logging.error(f"Ride extraction error: {e}")
            
        return rides
    
    def _parse_price(self, price_text):
        import re
        # Extract numbers from price text like "₹150-180" or "$15-20"
        numbers = re.findall(r'\d+', price_text.replace(',', ''))
        if numbers:
            # Take average of range or single price
            prices = [int(n) for n in numbers]
            return sum(prices) / len(prices)
        return 0.0
    
    def _parse_eta(self, eta_text):
        import re
        # Extract minutes from text like "5 min" or "12 mins"
        minutes = re.findall(r'\d+', eta_text)
        if minutes:
            return int(minutes[0]) * 60  # Convert to seconds
        return 0

# Global automator instance
automator = MobileAppAutomator()

@app.route('/extract-uber', methods=['POST'])
def extract_uber():
    try:
        data = request.json
        pickup_lat = data['pickup_lat']
        pickup_lng = data['pickup_lng']
        dropoff_lat = data['dropoff_lat']
        dropoff_lng = data['dropoff_lng']
        
        rides = automator.extract_uber_data(pickup_lat, pickup_lng, dropoff_lat, dropoff_lng)
        return jsonify(rides)
        
    except Exception as e:
        logging.error(f"API error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route('/extract-all', methods=['POST'])
def extract_all():
    try:
        data = request.json
        pickup_lat = data['pickup_lat']
        pickup_lng = data['pickup_lng']
        dropoff_lat = data['dropoff_lat']
        dropoff_lng = data['dropoff_lng']
        
        results = []
        
        # Extract from multiple services in parallel
        with ThreadPoolExecutor(max_workers=3) as executor:
            uber_future = executor.submit(automator.extract_uber_data, 
                                        pickup_lat, pickup_lng, dropoff_lat, dropoff_lng)
            # Add other services here
            
            uber_results = uber_future.result(timeout=30)
            results.extend(uber_results)
        
        return jsonify(results)
        
    except Exception as e:
        logging.error(f"API error: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=True)
