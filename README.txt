Payment tracker
===============

Small HTTP service: records payments per currency, serves the running balance,
and prints all balances to the console on an interval. HTTP is MuServer.

  POST /payments/{currency}/{amount}   ->  200 {"currency":"USD","amount":900.00}
  GET  /payments/{currency}            ->  200 {"currency":"USD","amount":900.00}

  currency = 3 uppercase letters, amount = a decimal, +/- allowed.


How to build and run
--------------------
Needs JDK 21+ and Maven; the first build needs internet to fetch dependencies.
Check the toolchain:  java -version  (want 21 or higher)  and  mvn -version

  1. Unzip and enter the project:
       unzip payments-*.zip && cd payments
  2. Build (compiles, runs the tests, produces target/payments.jar):
       mvn clean package
       mvn clean package
  3. Run (port 8080, summary every 60s):
       java -jar target/payments.jar
     ...or override the defaults:
       java -jar target/payments.jar --interval=5 --port=9090

  --interval=N   summary interval in seconds (default 60)
  --port=N       HTTP port (default 8080)

Bad flags print usage to stderr and exit 2. Ctrl+C stops it (server and summary
thread shut down cleanly). If the port is already in use, startup fails with an
error and a non-zero exit - I don't catch that specially.

Tests only:  mvn test
Run without packaging:
  mvn compile exec:java -Dexec.mainClass=com.example.payments.App -Dexec.args="--interval=5"

Quick check (second terminal):
  curl -i -X POST localhost:8080/payments/HKD/100
  curl -i -X POST localhost:8080/payments/USD/-100
  curl -i -X POST localhost:8080/payments/USD/1000
  curl -i        localhost:8080/payments/USD      # -> {"currency":"USD","amount":900.00}
  curl -i        localhost:8080/payments/EUR      # -> 404
  curl -i -X POST localhost:8080/payments/us/100  # -> 400


Assumptions
-----------
- In-memory only; balances reset on restart. No persistence was asked for.
- Money is BigDecimal, never double - floats can't hold decimal money exactly.
  Amounts are unbounded (BigDecimal), negatives allowed, so a balance can go
  negative. Input precision is kept when summing.
- amount is whatever java.math.BigDecimal parses: plain decimals, a leading
  + or -, and scientific notation like 1e3 (= 1000) are all accepted; anything
  else is a 400.
- Currency is validated strictly as [A-Z]{3}. Lowercase is rejected (400), not
  auto-uppercased - I'd rather surface a client bug than hide it. Not checked
  against the real ISO-4217 list, so "ZZZ" is accepted.
- Output is shown at 2 dp, HALF_EVEN, matching the examples. That assumes
  2-minor-unit currencies; JPY (0dp) / BHD (3dp
- The summary is a weakly-consistent snapshot: correct per currency, but not a
  single instant across all of them. A consistent snapshot would mean locking
  out writers, which isn't worth it for a console dump.
- Single instance: state lives in this process, there's no auth, and it isn't
  meant to run behind a load balancer with shared state.
- Library logs go to stderr at WARN (see simplelogger.properties) so stdout is
  just the summary.

Thread safety: state is one ConcurrentHashMap; each POST does
merge(currency, amount, BigDecimal::add), which is atomic per key, so concurrent
writes to the same currency don't race and there's no explicit lock. There's a
stress test for this (8 threads x 100k increments sum exactly).


What I'd add or change with another 2 hours
-------------------------------------------
Roughly in the order I'd tackle them:
  1. Per-currency decimal scale from ISO-4217 (JPY 0dp, BHD 3dp) instead of a
     flat 2 dp, and validate the code is a real currency.
  2. More tests: property-based checks on the money math, a mixed read+write
     concurrency test, and explicit 405 / bad-path coverage.
  3. Firmer HTTP contract: RFC 7807 problem+json error bodies, and confirm 405
     for a known path with the wrong method. Likely return amount as a JSON
     string, not a number, so clients don't lose precision to float parsing.
  4. GET /payments for all balances, a /health endpoint, and accept the amount
     in the request body rather than the URL path (more idiomatic).

Given longer: persist as an append-only event log so the balance becomes a
derived, auditable view that survives restarts; add metrics (Micrometer) and a
timestamp on each summary block.
