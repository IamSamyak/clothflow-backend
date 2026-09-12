#!/usr/bin/env bash

set -e

CLUSTER="clothflow"
PROFILE="clothflow"
REGION="us-east-1"
ENDPOINT="http://127.0.0.1:4566"

SERVICES=(
  clothflow-product-service
  clothflow-inventory-service
  clothflow-order-service
  clothflow-payment-service
  clothflow-shipping-service
  clothflow-notification-service
  clothflow-user-service
)

for SERVICE in "${SERVICES[@]}"; do
  echo "Starting/redeploying $SERVICE..."

  aws ecs update-service \
    --cluster "$CLUSTER" \
    --service "$SERVICE" \
    --force-new-deployment \
    --profile "$PROFILE" \
    --region "$REGION" \
    --endpoint-url "$ENDPOINT" \
    --no-cli-pager
done

echo
echo "Waiting for ECS services..."

sleep 10

aws ecs describe-services \
  --cluster "$CLUSTER" \
  --services "${SERVICES[@]}" \
  --profile "$PROFILE" \
  --region "$REGION" \
  --endpoint-url "$ENDPOINT" \
  --query 'services[].{Service:serviceName,Desired:desiredCount,Running:runningCount,Pending:pendingCount,Status:status}' \
  --output table \
  --no-cli-pager