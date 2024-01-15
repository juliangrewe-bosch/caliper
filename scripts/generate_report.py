import itertools
from datetime import datetime, timedelta, timezone

import matplotlib.dates as mdates
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
from prometheus_api_client import PrometheusConnect, MetricRangeDataFrame

# Path to the MkDocs folder
# home_dir = os.environ['HOME']
# AMPHORA_CHART_PATH = os.path.join(home_dir, 'caliper', 'mkdocs', 'docs', 'images', 'amphorasimulation')
# AMPHORA_CHART_PATH = os.path.join(home_dir, 'caliper', 'mkdocs', 'docs', 'images', 'ephemeralsimulation')

APOLLO_PROMETHEUS_CLIENT = PrometheusConnect(url="http://20.54.34.230:9090", disable_ssl=True)
STARBUCK_PROMETHEUS_CLIENT = PrometheusConnect(url="http://20.54.34.230:9090", disable_ssl=True)

END_TIME = datetime.now(timezone.utc)  # Prometheus uses UTC
START_TIME = END_TIME - timedelta(hours=5)

# TODO groups="*"
AMPHORA_SIMULATION_GROUPS = 'caliper{simulation="amphorasimulation", metric="percentiles99", scope="ok"}'
EPHEMERAL_SIMULATION_GROUPS = 'caliper{simulation="ephemeralsimulation", metric="percentiles99", scope="ok"}'

AMPHORA_SERVICES = [("amphora", "cs-amphora"), ("castor", "cs-castor")]
EPHEMERAL_SERVICES = [("ephemeral-ephemeral", "ephemeral"), ("castor", "cs-castor")]

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


def generate_cadvisor_metrics(services):
    all_metrics = []

    for container, pod in services:
        metric = [metric_query.format(container=container, pod=pod)
                  for metric_query in CADVISOR_METRICS_TEMPLATE]
        all_metrics.extend(metric)

    return all_metrics


AMPHORA_CADVISOR_METRICS = generate_cadvisor_metrics(AMPHORA_SERVICES)
EPHEMERAL_CADVISOR_METRICS = generate_cadvisor_metrics(EPHEMERAL_SERVICES)

amphora_simulation_groups_dict = APOLLO_PROMETHEUS_CLIENT.custom_query_range(query=AMPHORA_SIMULATION_GROUPS,
                                                                             start_time=START_TIME,
                                                                             end_time=END_TIME, step='15s')

ephemeral_simulation_groups_dict = APOLLO_PROMETHEUS_CLIENT.custom_query_range(query=EPHEMERAL_SIMULATION_GROUPS,
                                                                               start_time=START_TIME,
                                                                               end_time=END_TIME, step='15s')
# Time ranges per group
amphora_simulation_groups_df = MetricRangeDataFrame(amphora_simulation_groups_dict)
amphora_simulation_groups_df["timestamp"] = amphora_simulation_groups_df.index

ephemeral_simulation_groups_df = MetricRangeDataFrame(ephemeral_simulation_groups_dict)
ephemeral_simulation_groups_df["timestamp"] = amphora_simulation_groups_df.index

amphora_simulation_groups_start_time = amphora_simulation_groups_df.groupby("group")['timestamp'].min()
amphora_simulation_groups_end_time = amphora_simulation_groups_df.groupby("group")['timestamp'].max()

ephemeral_simulation_groups_start_time = ephemeral_simulation_groups_df.groupby("group")['timestamp'].min()
ephemeral_simulation_groups_end_time = ephemeral_simulation_groups_df.groupby("group")['timestamp'].max()

for group in itertools.chain(amphora_simulation_groups_df['group'].drop_duplicates(),
                             ephemeral_simulation_groups_df['group'].drop_duplicates()):
    for metric_name, promQL in zip(CADVISOR_METRICS, AMPHORA_CADVISOR_METRICS):
        apollo_cAdvisor_metrics_dict = APOLLO_PROMETHEUS_CLIENT.custom_query_range(query=promQL,
                                                                                   start_time=START_TIME,
                                                                                   end_time=END_TIME, step='15s')

        apollo_cAdvisor_metrics_df = MetricRangeDataFrame(apollo_cAdvisor_metrics_dict)

        # Plotting the data
        plt.figure(figsize=(11, 7))
        plt.plot(apollo_cAdvisor_metrics_df.index, apollo_cAdvisor_metrics_df['value'], label='Apollo')

        # Highlighting the area from the top to bottom of the plot, not limited to the line
        plt.fill_betweenx([min(apollo_cAdvisor_metrics_df['value']), max(apollo_cAdvisor_metrics_df['value'])],
                          (amphora_simulation_groups_start_time[group] - timedelta(minutes=2)),
                          amphora_simulation_groups_end_time[group], color='orange', alpha=0.15)

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
        plt.gca().yaxis.set_major_formatter(ticker.FuncFormatter(lambda value, pos: (
            f'{value / 1e9:.1f} GB' if value >= 1e9 else
            f'{value / 1e6:.0f} MB' if value >= 1e6 else
            f'{value / 1e3:.0f} KB' if value >= 1e3 else
            f'{value:.0f}'
        )))
        plt.legend(loc='best')
        # adjust padding
        plt.subplots_adjust(left=0.1)

        # if not os.path.isdir(AMPHORA_CHART_PATH):
        #     os.makedirs(AMPHORA_CHART_PATH)
        plt.savefig(group + "_" + metric_name)
        plt.close()
