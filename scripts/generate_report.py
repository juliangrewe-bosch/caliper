import os
from datetime import datetime, timedelta

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
from prometheus_api_client import PrometheusConnect, MetricRangeDataFrame

APOLLO_PROMETHEUS_CLIENT = PrometheusConnect(
    url=f"http://{os.environ['APOLLO_NODE_IP']}:{os.environ['PROMETHEUS_METRICS_PORT']}", disable_ssl=True)

STARBUCK_PROMETHEUS_CLIENT = PrometheusConnect(
    url=f"http://{os.environ['STARBUCK_NODE_IP']}:{os.environ['PROMETHEUS_METRICS_PORT']}", disable_ssl=True)

PROMETHEUS_RESOURCE_CONSUMPTION_QUERIES = [
    # container_memory_working_set_bytes
    ('container_memory_working_set_bytes_amphora',
     'container_memory_working_set_bytes{container="amphora", pod=~"cs-amphora.*", service="kubelet"}'),
    ('container_memory_working_set_bytes_castor',
     'container_memory_working_set_bytes{container="castor", pod=~"cs-castor.*", service="kubelet"}'),
    ('container_memory_working_set_bytes_ephemeral',
     'container_memory_working_set_bytes{container="ephemeral-ephemeral", pod=~"ephemeral.*", service="kubelet"}'),
    # container_cpu_usage_seconds_total
    ('container_cpu_usage_seconds_total_amphora',
     'irate(container_cpu_usage_seconds_total{container="amphora", pod=~"cs-amphora.*", service="kubelet"}[5m])'),
    ('container_cpu_usage_seconds_total_castor',
     'irate(container_cpu_usage_seconds_total{container="castor", pod=~"cs-castor.*", service="kubelet"}[5m])'),
    ('container_cpu_usage_seconds_total_ephemeral',
     'irate(container_cpu_usage_seconds_total{container="ephemeral-ephemeral", pod=~"ephemeral.*", ' \
     'service="kubelet"}[5m])'),
    # container_fs_writes_bytes_total
    ('container_fs_writes_bytes_total_amphora',
     'container_fs_writes_bytes_total{container="amphora", pod=~"cs-amphora.*", service="kubelet"}'),
    ('container_fs_writes_bytes_total_castor',
     'container_fs_writes_bytes_total{container="castor", pod=~"cs-castor.*", service="kubelet"}'),
    ('container_fs_writes_bytes_total_ephemeral',
     'container_fs_writes_bytes_total{container="ephemeral-ephemeral", pod=~"ephemeral.*", ' \
     'service="kubelet"}'),
    # container_fs_reads_bytes_total
    ('container_fs_reads_bytes_total_amphora',
     'container_fs_reads_bytes_total{container="amphora", pod=~"cs-amphora.*", service="kubelet"}'),
    ('container_fs_reads_bytes_total_castor',
     'container_fs_reads_bytes_total{container="castor", pod=~"cs-castor.*", service="kubelet"}'),
    ('container_fs_reads_bytes_total_ephemeral',
     'container_fs_reads_bytes_total{container="ephemeral-ephemeral", pod=~"ephemeral.*", ' \
     'service="kubelet"}'),
    # container_network_receive_bytes_total
    ('container_network_receive_bytes_total_amphora',
     'container_network_receive_bytes_total{pod=~"cs-amphora.*", service="kubelet"}'),
    ('container_network_receive_bytes_total_castor',
     'container_network_receive_bytes_total{pod=~"cs-castor.*", service="kubelet"}'),
    ('container_network_receive_bytes_total_ephemeral',
     'container_network_receive_bytes_total{pod=~"ephemeral.*", service="kubelet"}'),
    # container_network_transmit_bytes_total
    ('container_network_transmit_bytes_total_amphora',
     'container_network_transmit_bytes_total{pod=~"cs-amphora.*", service="kubelet"}'),
    ('container_network_transmit_bytes_total_castor',
     'container_network_transmit_bytes_total{pod=~"cs-castor.*", service="kubelet"}'),
    ('container_network_transmit_bytes_total_ephemeral',
     'container_network_transmit_bytes_total{pod=~"ephemeral.*", service="kubelet"}')]

PROMETHEUS_LATENCY_METRICS = [
    ('amphora_latency',
     'gatling_carbynestacksimulation_amphora{scope="all", metric="percentiles99"}'),
    ('ephemeral_latency',
     'gatling_carbynestacksimulation_ephemeral{scope="all", metric="percentiles99"}')]

IMG_DIR = 'docs/assets/img/'
END_TIME = datetime.now()
START_TIME = END_TIME - timedelta(hours=6)
HTML_REPORT_TIMESTAMP = datetime.now().strftime("%B %d, %Y")

plots_html = ''
for query_name, query in PROMETHEUS_LATENCY_METRICS:
    apollo_metrics_data_dict = APOLLO_PROMETHEUS_CLIENT.custom_query_range(query=query, start_time=START_TIME,
                                                                           end_time=END_TIME, step='15s')

    apollo_metrics_data_df = MetricRangeDataFrame(apollo_metrics_data_dict).drop_duplicates()

    # Plotting
    plt.figure(figsize=(12, 7))
    plt.plot(apollo_metrics_data_df.index, apollo_metrics_data_df['value'] / 1000, 'ro')

    plt.gca().xaxis.set_major_formatter(plt.matplotlib.dates.DateFormatter('%H:%M'))
    plt.gca().xaxis.set_major_locator(plt.MaxNLocator(12))
    plt.gcf().autofmt_xdate()

    plt.title(query_name)
    plt.grid(True)
    plt.subplots_adjust(bottom=0.1, top=0.95)
    plot_path = os.path.join(IMG_DIR, f'{query_name}.png')
    img_src_path = f'assets/img/{query_name}.png'
    plt.savefig(plot_path)
    plt.close()

    plots_html += f'''
    <div class="graph-container">
        <img src="{img_src_path}" alt="{query_name}">
    </div>
    '''

for query_name, query in PROMETHEUS_RESOURCE_CONSUMPTION_QUERIES:
    apollo_metrics_data_dict = APOLLO_PROMETHEUS_CLIENT.custom_query_range(query=query, start_time=START_TIME,
                                                                           end_time=END_TIME, step='15s')

    starbuck_metrics_data_dict = STARBUCK_PROMETHEUS_CLIENT.custom_query_range(query=query, start_time=START_TIME,
                                                                               end_time=END_TIME, step='15s')

    # groupby id in case of pod restarts and multiple time series for one query
    apollo_metrics_data_df_grouped = MetricRangeDataFrame(apollo_metrics_data_dict).groupby('id')
    starbuck_metrics_data_df_grouped = MetricRangeDataFrame(starbuck_metrics_data_dict).groupby('id')

    # Plotting
    plt.figure(figsize=(12, 7))

    for label, df_grouped in [('Apollo', apollo_metrics_data_df_grouped),
                              ('Starbuck', starbuck_metrics_data_df_grouped)]:
        for _, df in df_grouped:
            plt.plot(df.index, df['value'], label=label)

    # Format the x-axis
    plt.gca().xaxis.set_major_formatter(plt.matplotlib.dates.DateFormatter('%H:%M'))
    if 'cpu' not in query_name:
        plt.gca().yaxis.set_major_formatter(ticker.FuncFormatter(lambda value, pos: (
            f'{value / 1e9:.1f} GB' if value >= 1e9 else
            f'{value / 1e6:.0f} MB' if value >= 1e6 else
            f'{value / 1e3:.0f} KB' if value >= 1e3 else
            f'{value:.0f}'
        )))
    plt.gca().xaxis.set_major_locator(plt.MaxNLocator(12))
    plt.gcf().autofmt_xdate()

    plt.title(query_name)
    plt.grid(True)
    plt.legend()
    plt.subplots_adjust(bottom=0.1, top=0.95)

    plot_path = os.path.join(IMG_DIR, f'{query_name}.png')
    img_src_path = f'assets/img/{query_name}.png'
    plt.savefig(plot_path)
    plt.close()

    plots_html += f'''
    <div class="graph-container">
        <img src="{img_src_path}" alt="{query_name}">
    </div>
    '''

# HTML Report
html_content = f'''
<!DOCTYPE html>
<html>
<head>
    <title>Caliper load-tests result</title>
    <link rel="stylesheet" type="text/css" href="assets/css/styles.css">
</head>
<body>
<div class="label-container">
    <span class="timestamp">Generated on {HTML_REPORT_TIMESTAMP}</span>
</div>

<div class="separator"></div>

<div class="graph-container-wrapper">

{plots_html}

</div>

</body>
</html>
'''

with open('docs/index.html', 'w') as file:
    file.write(html_content)