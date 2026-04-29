import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  stages: [
    { duration: "30s", target: 5 },
    { duration: "2m", target: 5 },
    { duration: "30s", target: 15 },
    { duration: "2m", target: 15 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1500"],
  },
};

function randomId() {
  return `${Date.now()}-${Math.floor(Math.random() * 1000000)}`;
}

function registerAndLogin() {
  const uid = randomId();
  const email = `load-${uid}@test.com`;
  const password = "load-pass-123";

  const registerPayload = JSON.stringify({
    username: `load-${uid}`,
    email,
    password,
  });

  const registerRes = http.post(
    `${BASE_URL}/api/auth/register`,
    registerPayload,
    {
      headers: { "Content-Type": "application/json" },
    },
  );

  check(registerRes, {
    "register status is 201": (r) => r.status === 201,
  });

  const loginPayload = JSON.stringify({ email, password });
  const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, {
    headers: { "Content-Type": "application/json" },
  });

  check(loginRes, {
    "login status is 200": (r) => r.status === 200,
    "login has accessToken": (r) => !!r.json("accessToken"),
  });

  return loginRes.json("accessToken");
}

export default function () {
  const token = registerAndLogin();

  const authHeaders = {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  };

  const meRes = http.get(`${BASE_URL}/api/auth/me`, authHeaders);
  check(meRes, {
    "GET /api/auth/me is 200": (r) => r.status === 200,
  });

  const usersRes = http.get(`${BASE_URL}/api/users`, {
    ...authHeaders,
    responseCallback: http.expectedStatuses(200, 403),
  });

  const contentRes = http.get(`${BASE_URL}/api/content`);
  check(contentRes, {
    "GET /api/content is 200": (r) => r.status === 200,
  });

  sleep(1);
}
