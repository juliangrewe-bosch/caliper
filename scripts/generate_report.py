import os
from datetime import datetime, timedelta

import matplotlib.pyplot as plt
from prometheus_api_client import PrometheusConnect, MetricRangeDataFrame

apollo_prometheus_client = PrometheusConnect(
    url=f"http://{os.environ['APOLLO_NODE_IP']}:{os.environ['PROMETHEUS_METRICS_PORT']}", disable_ssl=True)
starbuck_prometheus_starbuck = PrometheusConnect(
    url=f"http://{os.environ['STARBUCK_NODE_IP']}:{os.environ['PROMETHEUS_METRICS_PORT']}", disable_ssl=True)

# PromQL-Queries
end_time = datetime.now()
start_time = end_time - timedelta(hours=2)

queries = [
    # container_memory_working_set_bytes
    ('container_memory_working_set_bytes_amphora',
     'container_memory_working_set_bytes{container="amphora", pod=~"cs-amphora.*", service="kubelet"}'),
    ('container_memory_working_set_bytes_castor',
     'container_memory_working_set_bytes{container="castor", pod=~"cs-castor.*", service="kubelet"}'),
    # container_cpu_usage_seconds_total
    ('container_cpu_usage_seconds_total_amphora',
     'irate(container_cpu_usage_seconds_total{container="amphora", pod=~"cs-amphora.*", service="kubelet"}[1m])'),
    ('container_cpu_usage_seconds_total_castor',
     'irate(container_cpu_usage_seconds_total{container="castor", pod=~"cs-castor.*", service="kubelet"}[1m])'),
    # container_fs_writes_bytes_total
    ('container_fs_writes_bytes_total_amphora',
     'irate(container_fs_writes_bytes_total{container="amphora", pod=~"cs-amphora.*", service="kubelet"}[1m])'),
    ('container_fs_writes_bytes_total_castor',
     'irate(container_fs_writes_bytes_total{container="castor", pod=~"cs-castor.*", service="kubelet"}[1m])'),
    # container_fs_reads_bytes_total
    ('container_fs_reads_bytes_total_amphora',
     'irate(container_fs_reads_bytes_total{container="amphora", pod=~"cs-amphora.*", service="kubelet"}[1m])'),
    ('container_fs_reads_bytes_total_castor',
     'irate(container_fs_reads_bytes_total{container="castor", pod=~"cs-castor.*", service="kubelet"}[1m])'),
    # container_network_receive_bytes_total
    ('container_network_receive_bytes_total_amphora',
     'irate(container_network_receive_bytes_total{pod=~"cs-amphora.*", service="kubelet"}[1m])'),
    ('container_network_receive_bytes_total_castor',
     'irate(container_network_receive_bytes_total{pod=~"cs-castor.*", service="kubelet"}[1m])'),
    # container_network_transmit_bytes_total
    ('container_network_transmit_bytes_total_amphora',
     'irate(container_network_transmit_bytes_total{pod=~"cs-amphora.*", service="kubelet"}[1m])'),
    ('container_network_transmit_bytes_total_castor',
     'irate(container_network_transmit_bytes_total{pod=~"cs-castor.*", service="kubelet"}[1m])')]

plots_html = ''

for query_name, query in queries:
    apollo_metrics_data_dict = apollo_prometheus_client.custom_query_range(query=query, start_time=start_time,
                                                                           end_time=end_time, step='15s')
    starbuck_metrics_data_dict = starbuck_prometheus_starbuck.custom_query_range(query=query, start_time=start_time,
                                                                                 end_time=end_time, step='15s')

    apollo_metrics_data_df = MetricRangeDataFrame(apollo_metrics_data_dict)
    starbuck_metrics_data_df = MetricRangeDataFrame(starbuck_metrics_data_dict)

    # Plotting
    plt.figure(figsize=(12, 7))
    plt.plot(apollo_metrics_data_df.index, apollo_metrics_data_df['value'], label='Apollo')
    plt.plot(starbuck_metrics_data_df.index, starbuck_metrics_data_df['value'], label='Starbuck')
    # Format the x-axis
    plt.gca().xaxis.set_major_formatter(plt.matplotlib.dates.DateFormatter('%H:%M'))
    plt.gca().xaxis.set_major_locator(plt.MaxNLocator(12))
    plt.gcf().autofmt_xdate()

    plt.title(query_name)
    plt.grid(True)
    plt.legend()
    plt.subplots_adjust(left=0.04, right=0.99, bottom=0.1, top=0.95)

    plot_path = 'assets/img/' + query_name + '.png'
    plt.savefig(plot_path)
    plt.close()

    plots_html += f'''
    <div class="graph-container">
        <img src="{plot_path}" alt="{query_name}">
    </div>
    '''

# HTML Report
timestamp = datetime.now().strftime("%B %d, %Y")

html_content = f'''
<!DOCTYPE html>
<html>
<head>
    <title>Caliper load-tests result</title>
    <link rel="stylesheet" type="text/css" href="assets/css/styles.css">
</head>
<body>
<div class="label-container">
    <span class="timestamp">Generated on {timestamp}</span>
</div>

<div class="separator"></div>

<div class="graph-container-wrapper">

{plots_html}

</div>

<iframe src="subpage/index.html" frameborder="0"></iframe>

</body>
</html>
'''

with open('index.html', 'w') as file:
    file.write(html_content)
