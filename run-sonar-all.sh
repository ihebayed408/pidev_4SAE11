#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────
# run-sonar-all.sh — Run JaCoCo + SonarQube analysis for ALL services
#
# Usage:
#   ./run-sonar-all.sh <SONAR_TOKEN>         Full analysis (tests + coverage + SonarQube)
#   ./run-sonar-all.sh --coverage-only       JaCoCo reports only (no SonarQube needed)
#
# Prerequisites (full mode):
#   1. SonarQube running at http://localhost:9000
#   2. Generate a token: My Account > Security > Generate Token
# ──────────────────────────────────────────────────────────────────────
set -euo pipefail

SONAR_URL="http://localhost:9000"
ROOT="$(cd "$(dirname "$0")" && pwd)"
COVERAGE_ONLY=false

if [[ "${1:-}" == "--coverage-only" ]]; then
  COVERAGE_ONLY=true
elif [[ -n "${1:-}" ]]; then
  TOKEN="$1"
else
  echo "Usage: $0 <SONAR_TOKEN>  or  $0 --coverage-only"
  exit 1
fi

# All backend services (infrastructure + microservices)
SERVICES=(
  "backEnd/Eureka"
  "backEnd/ConfigServer"
  "backEnd/apiGateway"
  "backEnd/KeyCloak"
  "backEnd/Microservices/user"
  "backEnd/Microservices/Project"
  "backEnd/Microservices/Offer"
  "backEnd/Microservices/Contract"
  "backEnd/Microservices/Portfolio"
  "backEnd/Microservices/review"
  "backEnd/Microservices/planning"
  "backEnd/Microservices/task"
  "backEnd/Microservices/Notification"
  "backEnd/Microservices/gamification"
  "backEnd/Microservices/ticket-service"
  "backEnd/Microservices/FreelanciaJob"
  "backEnd/Microservices/Meeting"
  "backEnd/Microservices/Chat"
  "backEnd/Microservices/Subcontracting"
)

PASSED=()
FAILED=()
SKIPPED=()

if $COVERAGE_ONLY; then
  echo "════════════════════════════════════════════════════════════"
  echo "  Coverage Only — ${#SERVICES[@]} services"
  echo "════════════════════════════════════════════════════════════"
else
  echo "════════════════════════════════════════════════════════════"
  echo "  SonarQube Analysis — ${#SERVICES[@]} services"
  echo "  SonarQube URL: $SONAR_URL"
  echo "════════════════════════════════════════════════════════════"
fi

for svc in "${SERVICES[@]}"; do
  dir="$ROOT/$svc"
  name="$(basename "$svc")"

  if [ ! -f "$dir/pom.xml" ]; then
    echo "  SKIP  $name — no pom.xml"
    SKIPPED+=("$name")
    continue
  fi

  echo ""
  if $COVERAGE_ONLY; then
    echo "── [$name] Running tests + JaCoCo ──"
    if (cd "$dir" && mvn clean test jacoco:report \
          -Dmaven.test.failure.ignore=true \
          -q 2>&1 | tail -3); then
      echo "  PASS  $name"
      PASSED+=("$name")
    else
      echo "  FAIL  $name"
      FAILED+=("$name")
    fi
  else
    echo "── [$name] Running tests + JaCoCo + SonarQube ──"
    if (cd "$dir" && mvn clean verify sonar:sonar \
          -Dsonar.token="$TOKEN" \
          -Dsonar.host.url="$SONAR_URL" \
          -Dsonar.projectKey="$name" \
          -Dsonar.projectName="$name" \
          -Dmaven.test.failure.ignore=true \
          -q 2>&1 | tail -5); then
      echo "  PASS  $name"
      PASSED+=("$name")
    else
      echo "  FAIL  $name"
      FAILED+=("$name")
    fi
  fi
done

echo ""
echo "════════════════════════════════════════════════════════════"
echo "  RESULTS"
echo "════════════════════════════════════════════════════════════"
echo "  Passed:  ${#PASSED[@]}  —  ${PASSED[*]:-none}"
echo "  Failed:  ${#FAILED[@]}  —  ${FAILED[*]:-none}"
echo "  Skipped: ${#SKIPPED[@]}  —  ${SKIPPED[*]:-none}"
echo ""
if $COVERAGE_ONLY; then
  echo "  HTML reports: backEnd/Microservices/<service>/target/site/jacoco/index.html"
else
  echo "  Dashboard: $SONAR_URL/projects"
fi
echo "════════════════════════════════════════════════════════════"
