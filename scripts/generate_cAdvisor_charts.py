import os
from datetime import datetime, timedelta, timezone

import matplotlib.dates as mdates
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
from prometheus_api_client import PrometheusConnect, MetricRangeDataFrame

# Path to the MkDocs folder
home_dir = os.environ['HOME']
AMPHORA_CHART_PATH = os.path.join(home_dir, 'caliper', 'mkdocs', 'docs', 'images', 'charts', 'amphorasimulation')
EPHEMERAL_CHART_PATH = os.path.join(home_dir, 'caliper', 'mkdocs', 'docs', 'images', 'charts', 'ephemeralsimulation')

# Prometheus Server Address
APOLLO_NODE_IP = os.environ['APOLLO_NODE_IP']
STARBUCK_NODE_IP = os.environ['STARBUCK_NODE_IP']
PROMETHEUS_SERVER_PORT = os.environ['PROMETHEUS_SERVER_PORT']

APOLLO_PROMETHEUS_CLIENT = PrometheusConnect(url=f"http://{APOLLO_NODE_IP}:{PROMETHEUS_SERVER_PORT}",
                                             disable_ssl=True)
STARBUCK_PROMETHEUS_CLIENT = PrometheusConnect(url=f"http://{STARBUCK_NODE_IP}:{PROMETHEUS_SERVER_PORT}",
                                               disable_ssl=True)

END_TIME = datetime.now(timezone.utc)  # Prometheus uses UTC
START_TIME = END_TIME - timedelta(hours=1) #TODO change to 5

AMPHORA_SIMULATION_GROUPS = 'caliper{simulation="amphorasimulation", metric="percentiles99", scope="ok"}'
EPHEMERAL_SIMULATION_GROUPS = 'caliper{simulation="ephemeralsimulation", metric="percentiles99", scope="ok"}'

# The metric name is part of the file name
CADVISOR_METRICS = [
    'container_memory_working_set_bytes',
    'container_cpu_usage_seconds_total',
    'container_fs_writes_bytes_total',
    'container_fs_reads_bytes_total',
    'container_network_receive_bytes_total',
    'container_network_transmit_bytes_total']

CADVISOR_METRICS_TEMPLATE = [
    # container_memory_working_set_bytes
    'container_memory_working_set_bytes{{container="{container}", pod=~"{pod}.*", service="kubelet"}}',
    # container_cpu_usage_seconds_total
    'irate(container_cpu_usage_seconds_total{{container="{container}", pod=~"{pod}.*", service="kubelet"}}[5m])',
    # container_fs_writes_bytes_total
    'container_fs_writes_bytes_total{{container="{container}", pod=~"{pod}.*", service="kubelet"}}',
    # container_fs_reads_bytes_total
    'container_fs_reads_bytes_total{{container="{container}", pod=~"{pod}.*", service="kubelet"}}',
    # container_network_receive_bytes_total
    'container_network_receive_bytes_total{{pod=~"{pod}.*", service="kubelet"}}',
    # container_network_transmit_bytes_total
    'container_network_transmit_bytes_total{{pod=~"{pod}.*", service="kubelet"}}']


def generate_cadvisor_metrics(container, pod):
    all_metrics = []

    metric = [metric_query.format(container=container, pod=pod)
              for metric_query in CADVISOR_METRICS_TEMPLATE]
    all_metrics.extend(metric)

    return all_metrics


AMPHORA_CADVISOR_METRICS = generate_cadvisor_metrics(container="amphora", pod="cs-amphora")
CASTOR_CADVISOR_METRICS = generate_cadvisor_metrics(container="castor", pod="cs-castor")
EPHEMERAL_CADVISOR_METRICS = generate_cadvisor_metrics(container="ephemeral-ephemeral", pod="ephemeral")

amphora_simulation_groups_dict = APOLLO_PROMETHEUS_CLIENT.custom_query_range(query=AMPHORA_SIMULATION_GROUPS,
                                                                             start_time=START_TIME,
                                                                             end_time=END_TIME, step='15s')

ephemeral_simulation_groups_dict = APOLLO_PROMETHEUS_CLIENT.custom_query_range(query=EPHEMERAL_SIMULATION_GROUPS,
                                                                               start_time=START_TIME,
                                                                               end_time=END_TIME, step='15s')
# Time ranges per group
amphora_simulation_groups_df = MetricRangeDataFrame(amphora_simulation_groups_dict)
amphora_simulation_groups_df["timestamp"] = amphora_simulation_groups_df.index

# ephemeral_simulation_groups_df = MetricRangeDataFrame(ephemeral_simulation_groups_dict)
# ephemeral_simulation_groups_df["timestamp"] = amphora_simulation_groups_df.index

amphora_simulation_groups_start_times = amphora_simulation_groups_df.groupby("group")['timestamp'].min()
amphora_simulation_groups_end_times = amphora_simulation_groups_df.groupby("group")['timestamp'].max()

# ephemeral_simulation_groups_start_times = ephemeral_simulation_groups_df.groupby("group")['timestamp'].min()
# ephemeral_simulation_groups_end_times = ephemeral_simulation_groups_df.groupby("group")['timestamp'].max()


def create_charts_for_groups(groups, cadvisor_metrics_names, cadvisor_metrics_promQL, simulation_groups_start_times,
                             simulation_groups_end_times, dir, service_name):
    """
    Plots metrics for specified groups and metrics.

    :param groups: Series containing group names.
    :param cadvisor_metrics_names: List with metric names.
    :param cadvisor_metrics_promQL: List with corresponding PromQL queries.
    :param simulation_groups_start_time: OBJECT with start times per group for a simulation
    :param simulation_groups_end_time: OBJECT with end time per group for a simulation
    :param dir: directory to store charts
    :param service_name: Name of the service.
    """
    for group in groups:
        for metric_name, promQL in zip(cadvisor_metrics_names, cadvisor_metrics_promQL):
            apollo_cAdvisor_metrics_dict = APOLLO_PROMETHEUS_CLIENT.custom_query_range(query=promQL,
                                                                                       start_time=START_TIME,
                                                                                       end_time=END_TIME, step='15s')

            starbuck_cAdvisor_metrics_dict = STARBUCK_PROMETHEUS_CLIENT.custom_query_range(query=promQL,
                                                                                           start_time=START_TIME,
                                                                                           end_time=END_TIME,
                                                                                           step='15s')

            apollo_cAdvisor_metrics_df = MetricRangeDataFrame(apollo_cAdvisor_metrics_dict)
            starbuck_cAdvisor_metrics_df = MetricRangeDataFrame(starbuck_cAdvisor_metrics_dict)

            # Plotting the data
            plt.figure(figsize=(12, 7))

            # Get the relevant data per group per cluster
            for label, df in [('Apollo', apollo_cAdvisor_metrics_df), ('Starbuck', starbuck_cAdvisor_metrics_df)]:
                cAdvisor_metrics_df_sliced = df[
                    (df.index >= (
                            simulation_groups_start_times[group] - timedelta(minutes=2))) &
                    (df.index <= simulation_groups_end_times[group])]

                plt.plot(cAdvisor_metrics_df_sliced.index, cAdvisor_metrics_df_sliced['value'], label=label)

            # Adding grid
            plt.grid(True, which='both', linestyle='--', linewidth=0.5)

            # Hiding the top and right spines
            ax = plt.gca()  # Get the current Axes instance
            ax.spines['top'].set_visible(False)
            ax.spines['right'].set_visible(False)

            # Set ticks direction to inward
            ax.tick_params(axis='both', which='both', direction='in')

            # Formatting the plot
            plt.tight_layout()
            plt.gca().xaxis.set_major_formatter(plt.matplotlib.dates.DateFormatter('%H:%M'))
            plt.gca().xaxis.set_major_locator(plt.MaxNLocator(12))
            if 'cpu' not in metric_name:
                plt.gca().yaxis.set_major_formatter(ticker.FuncFormatter(lambda value, pos: (
                    f'{value / 1e9:.2f} GB' if value >= 1e9 else
                    f'{value / 1e6:.2f} MB' if value >= 1e6 else
                    f'{value / 1e3:.2f} KB' if value >= 1e3 else
                    f'{value:.0f}'
                )))
            plt.legend(loc='best')
            # adjust padding
            plt.subplots_adjust(left=0.1)

            # Save the chart to the specified path
            file_name = f"{service_name}_{group}_{metric_name}"
            file_path = os.path.join(dir, service_name, file_name)
            plt.savefig(file_path)
            plt.close()


# Create charts for the amphora service from the amphorasimulation
create_charts_for_groups(groups=amphora_simulation_groups_df['group'].drop_duplicates(),
                         cadvisor_metrics_names=CADVISOR_METRICS,
                         cadvisor_metrics_promQL=AMPHORA_CADVISOR_METRICS,
                         simulation_groups_start_times=amphora_simulation_groups_start_times,
                         simulation_groups_end_times=amphora_simulation_groups_start_times,
                         dir=AMPHORA_CHART_PATH,
                         service_name="amphora")

# Create charts for the castor service from the amphorasimulation
create_charts_for_groups(groups=amphora_simulation_groups_df['group'].drop_duplicates(),
                         cadvisor_metrics_names=CADVISOR_METRICS,
                         cadvisor_metrics_promQL=CASTOR_CADVISOR_METRICS,
                         simulation_groups_start_times=amphora_simulation_groups_start_times,
                         simulation_groups_end_times=amphora_simulation_groups_start_times,
                         dir=AMPHORA_CHART_PATH,
                         service_name="castor")

# Create charts for the castor service from the amphorasimulation
# create_charts_for_groups(groups=ephemeral_simulation_groups_df['group'].drop_duplicates(),
#                          cadvisor_metrics_names=CADVISOR_METRICS,
#                          cadvisor_metrics_promQL=EPHEMERAL_CADVISOR_METRICS,
#                          simulation_groups_start_times=ephemeral_simulation_groups_start_times,
#                          simulation_groups_end_times=ephemeral_simulation_groups_start_times,
#                          path=EPHEMERAL_CHART_PATH,
#                          service_name="ephemeral")

# Create charts for the castor service from the amphorasimulation
# create_charts_for_groups(groups=ephemeral_simulation_groups_df['group'].drop_duplicates(),
#                          cadvisor_metrics_names=CADVISOR_METRICS,
#                          cadvisor_metrics_promQL=CASTOR_CADVISOR_METRICS,
#                          simulation_groups_start_times=ephemeral_simulation_groups_start_times,
#                          simulation_groups_end_times=ephemeral_simulation_groups_start_times,
#                          path=EPHEMERAL_CHART_PATH,
#                          service_name="castor")
