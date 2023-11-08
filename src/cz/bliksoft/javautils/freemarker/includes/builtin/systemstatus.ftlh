<#import "html.ftlh" as html>
<@html.additionalHead>
<@html.title>Status repot</@html.title>
<@html.chartjs />
</@html.additionalHead>
<@html.documentHeader><h1>System report (generated at ${.now?datetime})</h1></@html.documentHeader>
<@html.documentFooter class='noprint'>
<span class="button" id="expander_debug_pm" onclick="toggle_collapse('.expander_debug')">debug &plusmn;</span>
<div class="expander_debug " style="display: none;">
	<h1>HTTP input:</h1><div class="monospaced pre-wrap">${DescribeVariable(http.REQUEST, 'v', '|', '^')}</div>
</div>
</@html.documentFooter>

<@html.simpleDocument>
	<table style="display:inline-block;">
		<tbody>
			<tr><th colspan="2" class="center">Memory</th></tr>
		</tbody>
		<tbody>
			<tr><td class="right">used</td><td class="right">${(MEMORY_USED)?c} MB</td></tr>
			<tr><td class="right">free</td><td class="right">${(MEMORY_FREE)?c} MB</td></tr>
			<tr><td class="right">total</td><td class="right">${(MEMORY_TOTAL)?c} MB</td></tr>
		<tr><td class="right">max</td><td class="right">${(MEMORY_MAX)?c} MB</td></tr>
		</tbody>
		<tbody>
			<tr><th colspan="2" class="center">Host</th></tr>
		</tbody>
		<tbody>
			<tr><td class="right">Java version</td><td class="right">${JAVA_VERSION}</td></tr>
			<tr><td class="right">Hostname</td><td class="right">${HOSTNAME}</td></tr>
			<tr><td class="right">OS</td><td class="right">${OS}</td></tr>
		</tbody>
		<#list IPs>
		<tbody>
			<tr><th colspan="2" class="center">IP</th></tr>
		</tbody>
		<tbody>
		<#items as ip>
			<tr><td colspan="2" class="right">${ip}</td></tr>
		</#items>
		</tbody>
	</#list>
	</table>
  	<span style="width: 200px; height:200px;"><canvas id="memChart" style="width: 200px; height:200px;"></canvas></span>
  	<span><canvas id="memHistoryChart" style="width: 100%; height:400px;"></canvas></span>
<script>

	var chrt = $('#memChart');
      var chartId = new Chart(chrt, {
         type: 'pie',
         data: {
            labels: ["free", "used", "remaining max"],
            datasets: [{
               label: "Current memory usage",
               data: [${(MEMORY_FREE)?round?c}, ${(MEMORY_USED)?round?c}, ${(MEMORY_MAX-MEMORY_TOTAL)?round?c}],
               backgroundColor: ['lightgreen', 'yellow', 'aqua'],
               hoverOffset: 5
            }],
         },
         options: {
            responsive: false,
		    hover: {
              animationDuration: 0 // duration of animations when hovering an item
        	},
		    animation: {
			  duration: 0
			},
			responsiveAnimationDuration: 0,
         },
      });

	var chrt2 = $('#memHistoryChart');
      var chartId = new Chart(chrt2, {
         type: 'line',
         data: {
         	labels: [
         		<#list MEMORY as rep>'${rep.TS.format('hh:mm:ss')}',</#list>
         	],
            datasets: [
            {
               label: "free",
               data: [
               	<#list MEMORY as rep>'${(rep.FREE/1024/1024)?round?c}',</#list>
               ],
               borderColor: 'lightgreen',
            },
            {
               label: "used",
               data: [
               	<#list MEMORY as rep>'${(rep.USED/1024/1024)?round?c}',</#list>
               ],
               borderColor: 'yellow',
            },
            {
               label: "total",
               data: [
               	<#list MEMORY as rep>'${(rep.TOTAL/1024/1024)?round?c}',</#list>
               ],
               borderColor: 'aqua',
            }
            ]
         },
         options: {
            responsive: false,
            plugins: {
		      legend: {
		        position: 'top',
		      },
		    },
		    title: {
		      display: true,
		      text: 'Memory consumption in time'
		    },
		    hover: {
              animationDuration: 0 // duration of animations when hovering an item
        	},
		    animation: {
			  duration: 0
			},
			responsiveAnimationDuration: 0,
         },
      });

</script>

</@html.simpleDocument>
