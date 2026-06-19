import http from 'k6/http';
import { check, sleep } from 'k6';

// The attack profile
export const options = {
    stages: [
        { duration: '10s', target: 50 },  // Ramp up to 50 concurrent users
        { duration: '30s', target: 500 }, // Hold the spike at 500 users
        { duration: '10s', target: 0 },   // Graceful scale down
    ],
};

export default function () {
    const url = 'http://localhost:8080/api/v1/transfers/transfer';

    // Generate a random user from 1 to 100 and pad it with leading zeros (e.g., 001, 042, 100)
    const randomUserNum = Math.floor(Math.random() * 100) + 1;
    const paddedUserNum = String(randomUserNum).padStart(3, '0');

    const payload = JSON.stringify({
        userId: `user-${paddedUserNum}`,
        amount: parseFloat((Math.random() * (500 - 10) + 10).toFixed(2)),
        destinationAccount: `012345${Math.floor(1000 + Math.random() * 9000)}`,
        destinationBankCode: '058',
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(url, payload, params);

    // Validate the response. 504 Gateway Timeout is an expected architectural behavior.
    check(res, {
        'Success or Controlled Timeout (200/201/202/504)': (r) => [200, 201, 202, 504].includes(r.status),
    });

    // sleep(Math.random() * (0.5 - 0.1) + 0.1);
}