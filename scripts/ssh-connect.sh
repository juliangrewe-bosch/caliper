# Update package repositories and install necessary tools
sudo apt-get update > /dev/null
sudo apt-get install sshpass unzip -y

# Initialize variables for retry logic
max_attempts=5
attempt=0
ssh_exit_code=1


# Loop until sshpass succeeds or max attempts are reached
while [[ $ssh_exit_code -ne 0 && $attempt -lt $max_attempts ]]; do
  ((attempt++))
  echo "SSH attempt $attempt of $max_attempts"

  # SSH into the remote server using sshpass and execute script
  sshpass -p ${{ secrets.ADMIN_PASSWORD }} ssh -o StrictHostKeyChecking=no caliper@${{ env.AZURE_VM_IP }} \
  "export CALIPER_PRIVATE_REPOS_PAT=${{ secrets.CALIPER_PRIVATE_REPOS_PAT }}; \
    export AZURE_CLIENT_ID=${{ secrets.AZURE_CLIENT_ID }}; \
    export AZURE_CLIENT_SECRET=${{ secrets.AZURE_CLIENT_SECRET }}; \
    export AZURE_T_ID=${{ secrets.AZURE_TENANT_ID }}; \
    export AZURE_SUBSCRIPTION_ID=${{ secrets.AZURE_SUBSCRIPTION_ID }}; \
    export GITHUB_USERNAME=juliangrewe-bosch; \
    export CALIPER_PAT=${{ secrets.CALIPER_PAT }}; \
    export PRIME=198766463529478683931867765928436695041; \
    export R=141515903391459779531506841503331516415; \
    export INVR=133854242216446749056083838363708373830; \
    export PROGRAM=ephemeral-generic.default; \
    export TUPLE_THRESHOLD=1000000; \
    bash -s" < scripts/run_caliper_load_tests.sh

  # Capture the exit code of sshpass
  ssh_exit_code=$?

  # Check if sshpass succeeded
  if [[ $ssh_exit_code -eq 0 ]]; then
    echo "SSH connection successful."
    break
  else
    echo "SSH connection failed with exit code $ssh_exit_code, retrying..."
    sleep 30 # Wait for 30 seconds before retrying
  fi
done
