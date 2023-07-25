import {Construct} from "constructs";
import {App, Fn, TerraformOutput, TerraformStack} from "cdktf";
import {AzurermProvider} from "@cdktf/provider-azurerm/lib/provider";
import {ResourceGroup} from "@cdktf/provider-azurerm/lib/resource-group";
//import {KubernetesCluster} from "@cdktf/provider-azurerm/lib/kubernetes-cluster";
import {LinuxVirtualMachine} from "@cdktf/provider-azurerm/lib/linux-virtual-machine";
import {NetworkInterface} from "@cdktf/provider-azurerm/lib/network-interface";
import {Subnet} from "@cdktf/provider-azurerm/lib/subnet";
import {VirtualNetwork} from "@cdktf/provider-azurerm/lib/virtual-network";
import {PublicIp} from "@cdktf/provider-azurerm/lib/public-ip";
import {NetworkSecurityGroup} from "@cdktf/provider-azurerm/lib/network-security-group";
import {
    NetworkInterfaceSecurityGroupAssociation
} from "@cdktf/provider-azurerm/lib/network-interface-security-group-association";
//import {VirtualNetworkPeering} from "@cdktf/provider-azurerm/lib/virtual-network-peering";
//import {PrivateDnsZone} from "@cdktf/provider-azurerm/lib/private-dns-zone";
//import {PrivateDnsZoneVirtualNetworkLink} from "@cdktf/provider-azurerm/lib/private-dns-zone-virtual-network-link";

class CaliperStack extends TerraformStack {
    constructor(scope: Construct, name: string) {
        super(scope, name);

        new AzurermProvider(this, "AzureRm", {
            features: {
                resourceGroup: {
                    preventDeletionIfContainsResources: false
                }
            }
        });

        const caliper_resource_group = new ResourceGroup(this, "caliper-rg", {
            name: "caliper-rg",
            location: "North Europe"
        });

        //Kubernetes
        /* const apollo_virtual_network = new VirtualNetwork(this, "apollo-vnet", {
             name: "apollo-vnet",
             addressSpace: ["10.1.0.0/16"],
             location: caliper_resource_group.location,
             resourceGroupName: caliper_resource_group.name
         });

         const apollo_subnet = new Subnet(this, "apollo-subnet", {
             name: "apollo-subnet",
             resourceGroupName: caliper_resource_group.name,
             virtualNetworkName: apollo_virtual_network.name,
             addressPrefixes: ["10.1.1.0/24"]
         });

         const starbuck_virtual_network = new VirtualNetwork(this, "starbuck-vnet", {
             name: "starbuck-vnet",
             addressSpace: ["10.2.0.0/16"],
             location: caliper_resource_group.location,
             resourceGroupName: caliper_resource_group.name
         });

         const starbuck_subnet = new Subnet(this, "starbuck-subnet", {
             name: "starbuck-subnet",
             resourceGroupName: caliper_resource_group.name,
             virtualNetworkName: starbuck_virtual_network.name,
             addressPrefixes: ["10.2.1.0/24"]
         });

         new VirtualNetworkPeering(this, "vnet-peering-apollo-to-starbuck", {
             name: "vnet-peering-apollo-to-starbuck",
             resourceGroupName: caliper_resource_group.name,
             virtualNetworkName: apollo_virtual_network.name,
             remoteVirtualNetworkId: starbuck_virtual_network.id
         });

         new VirtualNetworkPeering(this, "vnet-peering-starbuck-to-apollo", {
             name: "vnet-peering-starbuck-to-apollo",
             resourceGroupName: caliper_resource_group.name,
             virtualNetworkName: starbuck_virtual_network.name,
             remoteVirtualNetworkId: apollo_virtual_network.id
         });

         const caliper_private_dns_zone = new PrivateDnsZone(this, "caliper-private-dns-zone", {
             name: "privatelink." + caliper_resource_group.location + ".azmk8s.io",
             resourceGroupName: caliper_resource_group.name,
         });

         const apollo_k8s_cluster = new KubernetesCluster(this, "apollo-private", {
             name: "apollo-private",
             location: caliper_resource_group.location,
             resourceGroupName: caliper_resource_group.name,
             dnsPrefix: "dns",
             privateDnsZoneId: caliper_private_dns_zone.id,
             defaultNodePool: {
                 name: "agentpool",
                 nodeCount: 1,
                 vmSize: "Standard_D3_v2",
                 vnetSubnetId: apollo_subnet.id
             },
             identity: {
                 type: "UserAssigned",
                 identityIds: ["/subscriptions/a0d4b3b2-cbb5-42c5-b496-9b5a1c48daf0/resourceGroups/caliper-managed-identity-rg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/caliper-aks-managed-identity"]
             },
             privateClusterEnabled: true,
             privateClusterPublicFqdnEnabled: false
         });

         const starbuck_k8s_cluster = new KubernetesCluster(this, "starbuck-private", {
             name: "starbuck-private",
             location: caliper_resource_group.location,
             resourceGroupName: caliper_resource_group.name,
             dnsPrefix: "dns",
             privateDnsZoneId: caliper_private_dns_zone.id,
             defaultNodePool: {
                 name: "agentpool",
                 nodeCount: 1,
                 vmSize: "Standard_D3_v2",
                 vnetSubnetId: starbuck_subnet.id
             },
             identity: {
                 type: "UserAssigned",
                 identityIds: ["/subscriptions/a0d4b3b2-cbb5-42c5-b496-9b5a1c48daf0/resourceGroups/caliper-managed-identity-rg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/caliper-aks-managed-identity"]
             },
             privateClusterEnabled: true,
             privateClusterPublicFqdnEnabled: false
         });*/

        // VM
        const vm_virtual_network = new VirtualNetwork(this, "vm-vnet", {
            name: "vm-vnet",
            addressSpace: ["10.0.0.0/16"],
            location: caliper_resource_group.location,
            resourceGroupName: caliper_resource_group.name
        });

        const vm_subnet = new Subnet(this, "vm-subnet", {
            name: "vm-subnet",
            resourceGroupName: caliper_resource_group.name,
            virtualNetworkName: vm_virtual_network.name,
            addressPrefixes: ["10.0.1.0/24"]
        });

        const vm_public_ip = new PublicIp(this, "vm-public-ip", {
            name: "vm-public-ip",
            location: caliper_resource_group.location,
            resourceGroupName: caliper_resource_group.name,
            allocationMethod: "Dynamic"
        });

        const vm_network_interface = new NetworkInterface(this, "vm-network-interface", {
            name: "vm-network-interface",
            location: caliper_resource_group.location,
            resourceGroupName: caliper_resource_group.name,
            ipConfiguration: [{
                name: "vm-network-interface-configuration",
                subnetId: vm_subnet.id,
                privateIpAddressAllocation: "Dynamic",
                publicIpAddressId: vm_public_ip.id
            }]
        });

        const vm_network_security_group = new NetworkSecurityGroup(this, "vm-network-security-group", {
            name: "vm-network-security-group",
            location: caliper_resource_group.location,
            resourceGroupName: caliper_resource_group.name,
            securityRule: [{
                name: "SSH",
                priority: 1001,
                direction: "Inbound",
                access: "Allow",
                protocol: "Tcp",
                sourcePortRange: "*",
                destinationPortRange: "22",
                sourceAddressPrefix: "*",
                destinationAddressPrefix: "*"
            }]
        });

        new NetworkInterfaceSecurityGroupAssociation(this, "vm-network-interface-security-group-association", {
            networkInterfaceId: vm_network_interface.id,
            networkSecurityGroupId: vm_network_security_group.id
        });

        const caliper_linux_virtual_machine = new LinuxVirtualMachine(this, "caliper-vm", {
            name: "caliper-vm",
            location: caliper_resource_group.location,
            resourceGroupName: caliper_resource_group.name,
            networkInterfaceIds: [vm_network_interface.id],
            size: "Standard_DS2_v2",
            osDisk: {
                caching: "ReadWrite",
                storageAccountType: "Standard_LRS"
            },
            sourceImageReference: {
                publisher: "Canonical",
                offer: "0001-com-ubuntu-server-jammy",
                sku: "22_04-lts-gen2",
                version: "latest"
            },
            computerName: "caliper-vm",
            adminUsername: "caliper",
            adminPassword: "ADMINadmin123456!?",
            disablePasswordAuthentication: false,
            customData: Fn.base64encode(Fn.rawString(
                "#cloud-config\n" +
                "packages:\n" +
                "  - openjdk-8-jdk\n" +
                "  - npm\n" +
                "  - nodejs\n" +
                "write_files:\n" +
                "  - path: /.m2/settings.xml\n" +
                "    content: |\n" +
                "      <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "      <settings xmlns=\"http://maven.apache.org/SETTINGS/1.2.0\"\n" +
                "                xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "                xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.2.0 http://maven.apache.org/xsd/settings-1.2.0.xsd\">\n" +
                "         <servers>\n" +
                "          <server>\n" +
                "            <id>github</id>\n" +
                "            <username>juliangrewe-bosch</username>\n" +
                "            <password>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</password>\n" +
                "          </server>\n" +
                "        </servers>\n" +
                "      </settings>\n" +
                "runcmd:\n" +
                "  - curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash\n" +
                "  - curl -sLO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl\n" +
                "  - chmod +x kubectl\n" +
                "  - mv kubectl /usr/local/bin\n" +
                "  - mv /.m2 /home/caliper/.m2\n" +
                "  - chown -R caliper:caliper /home/caliper/.m2/"))
            //  dependsOn: [apollo_k8s_cluster, starbuck_k8s_cluster]
        });
        /*
                new VirtualNetworkPeering(this, "vnet-peering-vm-to-apollo", {
                    name: "vnet-peering-vm-to-apollo",
                    resourceGroupName: caliper_resource_group.name,
                    virtualNetworkName: vm_virtual_network.name,
                    remoteVirtualNetworkId: apollo_virtual_network.id
                });

                new VirtualNetworkPeering(this, "vnet-peering-apollo-to-vm", {
                    name: "vnet-peering-apollo-to-vm",
                    resourceGroupName: caliper_resource_group.name,
                    virtualNetworkName: apollo_virtual_network.name,
                    remoteVirtualNetworkId: vm_virtual_network.id
                });

                new VirtualNetworkPeering(this, "vnet-peering-vm-to-starbuck", {
                    name: "vnet-peering-vm-to-starbuck",
                    resourceGroupName: caliper_resource_group.name,
                    virtualNetworkName: vm_virtual_network.name,
                    remoteVirtualNetworkId: starbuck_virtual_network.id
                });

                new VirtualNetworkPeering(this, "vnet-peering-starbuck-to-vm", {
                    name: "vnet-peering-starbuck-to-vm",
                    resourceGroupName: caliper_resource_group.name,
                    virtualNetworkName: starbuck_virtual_network.name,
                    remoteVirtualNetworkId: vm_virtual_network.id
                });

                new PrivateDnsZoneVirtualNetworkLink(this, "caliper-vm-vnet-caliper-private-dns-zone-link", {
                    name: "caliper-vm-vnet-caliper-private-dns-zone-link",
                    resourceGroupName: caliper_resource_group.name,
                    privateDnsZoneName: caliper_private_dns_zone.name,
                    virtualNetworkId: vm_virtual_network.id,
                    registrationEnabled: true
                });*/

        // Output
        new TerraformOutput(this, "caliper-vm-public-ip", {
            value: caliper_linux_virtual_machine.publicIpAddress,
        });
    }
}

const app = new App();
new CaliperStack(app, "caliper-azure-deployment");
app.synth();
