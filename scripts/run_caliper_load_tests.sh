#!/bin/bash

# TODO github account needed (with relevant privleges) to download carbyne stack clients
# Set up access to Carbynestacks Github Packages
mkdir -p ~/.m2
echo -e "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" \
  "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.2.0\"\n" \
  "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" \
  "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.2.0 http://maven.apache.org/xsd/settings-1.2.0.xsd\">\n" \
  "  <servers>\n" \
  "    <server>\n" \
  "      <id>github</id>\n" \
  "      <username>juliangrewe-bosch</username>\n" \
  "      <password>$CALIPER_PAT</password>\n" \
  "    </server>\n" \
  "  </servers>\n" \
  "</settings>" >.m2/settings.xml

# Configure HashiCorp GPG key and repository
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg >/dev/null
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list >/dev/null

# Configure Kubectl GPG key and repository
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.28/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg >/dev/null
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.28/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list >/dev/null

# Install NodeSource PPA
curl -s https://deb.nodesource.com/setup_18.x | sudo bash >/dev/null

# Update repositories and install required packages
sudo apt-get update >/dev/null
sudo apt-get install -y openjdk-8-jdk nodejs terraform kubectl python3-pip jq zip >/dev/null

# Install Azure-CLI
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash >/dev/null

# Install cdktf
sudo npm install --global cdktf-cli@0.16.3 >/dev/null

# Clone repositories
git clone https://github.com/juliangrewe-bosch/caliper.git
git clone https://$CALIPER_PRIVATE_REPOS_PAT@github.com/juliangrewe-bosch/carbynestack.git
# temporär
cd carbynestack
git checkout -b cdktf-caliper origin/cdktf-caliper
cd /home/caliper/caliper
git checkout -b caliper-workflow origin/caliper-workflow
cd /home/caliper

# Authenticate Terraform to Azure
az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_CLIENT_SECRET" --tenant "$AZURE_T_ID" --output none

# Download Prometheus Operator bundle
LATEST=$(curl -s https://api.github.com/repos/prometheus-operator/prometheus-operator/releases/latest | jq -cr .tag_name)
curl -o carbynestack/deployments/manifests/prometheus-operator-bundle.yaml -sL https://github.com/prometheus-operator/prometheus-operator/releases/download/"${LATEST}"/bundle.yaml

# Install dependencies and synthesize infrastructure using cdktf
cd carbynestack/deployments || exit 1
npm install >/dev/null
cdktf get >/dev/null
cdktf synth >/dev/null

# Initialize and apply Terraform
export TF_VAR_azureSubscriptionID=$AZURE_SUBSCRIPTION_ID
terraform -chdir=cdktf.out/stacks/private-aks-virtual-cloud/ init -input=false >/dev/null
terraform -chdir=cdktf.out/stacks/private-aks-virtual-cloud/ apply -auto-approve -input=false >/dev/null

# Get access credentials for private aks cluster
az aks get-credentials --name apollo-private --resource-group caliper-rg
az aks get-credentials --name starbuck-private --resource-group caliper-rg

# Run load-tests
cd /home/caliper/caliper || exit 1
export STARBUCK_FQDN=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}').sslip.io
kubectl config use-context apollo-private
export APOLLO_FQDN=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}').sslip.io
export PRIME=198766463529478683931867765928436695041 # TODO make env variables
export R=141515903391459779531506841503331516415
export INVR=133854242216446749056083838363708373830
export PROGRAM="ephemeral-generic.default"

kubectl patch tuplegenerationscheduler cs-klyshko-tuplegenerationscheduler -p '{"spec":{"threshold":1000000, "concurrency": 10}}' --type=merge
while true; do
  tuples_available=$(curl -s http://"$APOLLO_FQDN"/castor/intra-vcp/telemetry | jq '.metrics[] | select(.type == "INPUT_MASK_GFP") | .available')

  if [[ $tuples_available -ge 1000000 ]]; then
    break
  else
    sleep 300
  fi
done

chmod +x mvnw
./mvnw -q gatling:test

# Generate report
export PROMETHEUS_METRICS_PORT=32767 # TODO rename to PROMETHEUS_SERVER?
export APOLLO_NODE_IP=$(kubectl get node -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')
kubectl config use-context starbuck-private
export STARBUCK_NODE_IP=$(kubectl get node -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}')

pip3 install -r scripts/requirements.txt
python3 scripts/generate_report.py

# call webhook to commit, push and deploy the report

# create PAT with repo scope?
#TOKEN="your_github_token_here"
#REPO="juliangrewe-bosch/caliper"
#EVENT_TYPE="update_report"

# Call the GitHub API
#curl -X POST -H "Accept: application/vnd.github.everest-preview+json" \
#    -H "Authorization: token $TOKEN" \
#    --data "{\"event_type\": \"$EVENT_TYPE\"}" \
#    https://api.github.com/repos/$REPO/dispatches