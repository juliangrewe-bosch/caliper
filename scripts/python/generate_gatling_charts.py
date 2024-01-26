import logging.config
import os
from datetime import datetime, timedelta, timezone

import numpy as np
import yaml
from matplotlib import pyplot as plt, ticker
from prometheus_api_client import PrometheusConnect, MetricRangeDataFrame

logging.config.fileConfig('logging.conf')
logger = logging.getLogger('generate_gatling_charts')

# config file
with open('config.yaml', 'r') as file:
    config = yaml.safe_load(file)

APOLLO_NODE_IP = os.environ['APOLLO_NODE_IP']
PROMETHEUS_SERVER_PORT = os.environ['PROMETHEUS_SERVER_PORT']

APOLLO_PROMETHEUS_CLIENT = PrometheusConnect(url=f"http://{APOLLO_NODE_IP}:{PROMETHEUS_SERVER_PORT}",
                                             disable_ssl=True)

HOME_DIR = HOME_DIR = os.getcwd()  # os.environ['HOME'] # TODO change to home_env

END_TIME = datetime.now(timezone.utc)  # Prometheus uses UTC
START_TIME = END_TIME - timedelta(hours=config['simulation']['time_delta'])

AMPHORA_CHART_PATH = os.path.join(HOME_DIR, config['mkdocs']['charts']['amphora_simulation'])
EPHEMERAL_CHART_PATH = os.path.join(HOME_DIR, config['mkdocs']['charts']['ephemeral_simulation'])
REPORT_PATH = os.path.join(HOME_DIR, config['mkdocs']['report'])

AMPHORA_SIMULATION_CREATE_SECRET_GROUPS = config['simulation']['groups']['amphora']['createSecret']
AMPHORA_SIMULATION_GET_SECRET_GROUPS = config['simulation']['groups']['amphora']['getSecret']
AMPHORA_SIMULATION_DELETE_SECRET_GROUPS = config['simulation']['groups']['amphora']['deleteSecret']
EPHEMERAL_SIMULATION_GROUPS = config['simulation']['groups']['ephemeral']['execute']


def generate_markdown_table(data):
    markdown = ""

    # Calculate max length for each column
    max_lengths = [max(len(str(row[i])) for row in data) for i in range(len(data[0]))]

    # Add header
    header = data[0]
    markdown += "| " + " | ".join(header) + " |\n"

    # Add separator
    separator = ["-" * (max_lengths[i] + 2) for i in range(len(header))]
    markdown += "|" + "|".join(separator) + "|\n"

    # Add rows
    for row in data[1:]:
        row_formatted = [str(row[i]).ljust(max_lengths[i]) for i in range(len(row))]
        markdown += "| " + " | ".join(row_formatted) + " |\n"

    return markdown


for promQL in [AMPHORA_SIMULATION_CREATE_SECRET_GROUPS,
               AMPHORA_SIMULATION_GET_SECRET_GROUPS,
               AMPHORA_SIMULATION_DELETE_SECRET_GROUPS,
               EPHEMERAL_SIMULATION_GROUPS]:

    apollo_gatling_metrics_dict = APOLLO_PROMETHEUS_CLIENT.custom_query_range(
        query=promQL,
        start_time=START_TIME,
        end_time=END_TIME, step='15s')

    apollo_gatling_metrics_df = MetricRangeDataFrame(apollo_gatling_metrics_dict)
    # gatling sends the same metric in a configured interval, e.g. 1min
    apollo_gatling_metrics_df = apollo_gatling_metrics_df.drop_duplicates()

    apollo_gatling_metrics_df["timestamp"] = apollo_gatling_metrics_df.index

    plt.figure(figsize=(12, 7))

    colors = plt.cm.jet(np.linspace(0, 1, len(apollo_gatling_metrics_df)))

    for counter, (i, row) in enumerate(apollo_gatling_metrics_df.iterrows()):
        plt.scatter([row['timestamp']], [row['value']], color=colors[counter], label=row['group'])

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
        f'{value / 1e3:.2f} s' if value >= 1e3 else
        f'{value:.0f} ms'
    )))
    plt.legend(loc='best')
    # adjust padding
    plt.subplots_adjust(left=0.1)

    # Save the chart to the specified path
    file_name = 'getSecret_response_times'
    service = 'amphora'
    file_path = os.path.join(AMPHORA_CHART_PATH, service, file_name)
    plt.savefig(file_path)
    plt.close()

    response_times_statistics = apollo_gatling_metrics_df.groupby('group')['value'].agg(
        min='min',
        percentile_50=lambda x: x.quantile(0.5),
        percentile_75=lambda x: x.quantile(0.75),
        percentile_95=lambda x: x.quantile(0.95),
        percentile_99=lambda x: x.quantile(0.99),
        max='max',
        mean='mean',
        std_dev='std'
    )

    # Convert DataFrame to list of lists
    data = [['group'] + list(response_times_statistics.columns)]  # Header
    data += response_times_statistics.reset_index().values.tolist()  # Data rows

    filename = f"{REPORT_PATH}/{service}/{request}/{group}.md"
    markdown = generate_markdown_table(data)
    with open(filename, 'w') as file:
        file.write(markdown)

    logger.info(f"Generated {filename}")
