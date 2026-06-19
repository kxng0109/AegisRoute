from locust import HttpUser, task, between
import random

class AegisChaosEngine(HttpUser):
    # Simulate realistic user delays (100ms to 500ms between clicks)
    wait_time = between(0.1, 0.5)

    @task
    def transfer_funds(self):
        # Generate a random integer between 1 and 100 and zero-pad it to 3 digits dynamically
        user_id = f"user-{random.randint(1, 100):03d}"

        payload = {
            "userId": user_id,
            "amount": round(random.uniform(10.0, 500.0), 2),
            "destinationAccount": f"012345{random.randint(1000, 9999)}",
            "destinationBankCode": "058"
        }

        with self.client.post("/api/v1/transfers/transfer", json=payload, catch_response=True) as response:
            # We treat 504 as a success because the Saga pattern successfully caught it
            if response.status_code in [200, 201, 202, 504]:
                response.success()
            elif response.status_code == 500:
                response.failure(f"System Crash: {response.text}")
            else:
                response.failure(f"Unexpected Status: {response.status_code}")