from datetime import datetime, timedelta

import matplotlib.pyplot as plt
from prometheus_api_client import PrometheusConnect, MetricRangeDataFrame

apollo_prometheus_client = PrometheusConnect(
    url=f"http://20.82.175.211:9090", disable_ssl=True)
# prometheus_starbuck = PrometheusConnect(
# url=f"http://{os.environ['STARBUCK_NODE_IP']}:{os.environ['PROMETHEUS_METRICS_PORT']}", disable_ssl=True)

# PromQL-Queries
end_time = datetime.now()
start_time = end_time - timedelta(hours=2)

queries = [('container_memory_working_set_bytes',
            'container_memory_working_set_bytes{container="castor", pod=~"cs-castor.*", service="kubelet"}'),
           ('container_cpu_usage_seconds_total',
            'irate(container_cpu_usage_seconds_total{container="castor", pod=~"cs-castor.*", service="kubelet"}[1m])'),
           ('container_fs_writes_bytes_total',
            'irate(container_fs_writes_bytes_total{container="castor", pod=~"cs-castor.*", service="kubelet"}[1m])'),
           ('container_fs_reads_bytes_total',
            'irate(container_fs_reads_bytes_total{container="castor", pod=~"cs-castor.*", service="kubelet"}[1m])'),
           ('container_network_receive_bytes_total',
            'irate(container_network_receive_bytes_total{pod=~"cs-castor.*", service="kubelet"}[1m])'),
           ('container_network_transmit_bytes_total',
            'irate(container_network_transmit_bytes_total{pod=~"cs-castor.*", service="kubelet"}[1m])')]

plots_html = ''

for query_name, query in queries:
    metrics_data_dict = apollo_prometheus_client.custom_query_range(query=query, start_time=start_time,
                                                                    end_time=end_time, step='15s')
    metrics_data_df = MetricRangeDataFrame(metrics_data_dict)

    # Plotting
    plt.figure(figsize=(15, 6))
    plt.plot(metrics_data_df.index, metrics_data_df['value'])
    # Format the x-axis
    plt.gca().xaxis.set_major_formatter(plt.matplotlib.dates.DateFormatter('%H:%M'))
    plt.gca().xaxis.set_major_locator(plt.MaxNLocator(12))
    plt.gcf().autofmt_xdate()

    plt.title(query_name)
    plt.grid(True)

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
    <title>Apollo-Private</title>
    <link rel="stylesheet" type="text/css" href="assets/css/styles.css">
</head>
<body>
<div class="label-container">
    <span class="timestamp">Generated on {timestamp}</span>
</div>

<h1 class="title">Apollo-Private</h1>
<a href="subpage/index.html">Click here to open another index.html</a>

<div class="separator"></div>

{plots_html}

<iframe src="subpage/index.html" frameborder="0"></iframe>

</body>
</html>
'''

with open('index.html', 'w') as file:
    file.write(html_content)
