<#import "html.ftlh" as html>
<#include "builtin/functions.ftl">
<@html.additionalHead>
<@html.title><#if env.appName??>${env.appName}<#if env.envName??> (${env.envName})</#if> status<#else>App status report</#if></@html.title>
<@html.chartjs_inline />
<@html.jquery_inline />
<@html.timestamputils />
<#if GARBAGE!false> <meta http-equiv="refresh" content="15"></#if>
</@html.additionalHead>
<@html.documentHeader>
	<h1>System report (generated at ${.now?datetime?string["d.M.yyyy HH:mm:ss"]})<#if env.envName?? || env.appName??> <#if env.envName??>Env: ${env.envName} </#if><#if env.appName??>App: ${env.appName}</#if></#if></h1>
</@html.documentHeader>
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
		<tbody>
			<tr><th colspan="2" class="center">tools</th></tr>
			<tr><td colspan="2" class="right"><@html.inlineform><input type="text" size="10" name="reportname" placeholder="reportName" value="fromWeb"><@html.button 'save' 'save' 'true' /></@html.inlineform> <@html.inlineform><@html.button 'refresh' 'refresh' 'true' /></@html.inlineform><@html.inlineform><@html.button 'Run GC' 'collectGarbage' 'true' /></@html.inlineform></td></tr>
		</tbody>
	</table>
	<table style="display:inline-block;">
		<#if CPU?size gt 1>
			<tbody>
				<tr><th colspan="2" class="center">CPU</th><th class="right" width="100px">% CPU</th></tr>
			</tbody>
			<tbody>
					<#list threads>
					<#items as t>
					<tr <#if t.millis lt 0>class="striketrough"</#if>><td>${t.id?c}:${t.name}</td><td>${t.status!'?'}</td><td class="right"><#if t.millis lt 0><#else>${(t.millis/msBetweenSamples)?string("##0.00")!'?'}</#if></td></tr>
					</#items>
					</#list>
				<tr><td class="right">CPU total</td><td class="left">threads: ${threads?size}</td><td class="right">${(cpuNanos/msBetweenSamples/1000)?string("##0.00")}</td></tr>
			</tbody>
		<#else>
			<tbody>
				<tr><th colspan="2" class="center">CPU</th><th class="right" width="100px">ms</th></tr>
			</tbody>
			<tbody>
					<#list threads>
					<#items as t>
					<tr><td>${t.id?c}:${t.name}</td><td>${t.status!'?'}</td><td class="right">${t.millis?c!'?'}</td></tr>
					</#items>
					</#list>
				<tr><td class="right">CPU total</td><td class="left">threads: ${threads?size}</td><td class="right">${(cpuNanos/1000)?floor?c}</td></tr>
			</tbody>
		</#if>
	</table>
  	<span style="width: 200px; height:200px;"><canvas id="memChart" style="width: 200px; height:200px; display:inline-block;"></canvas></span>
  	<span><canvas id="memHistoryChart" style="width: 100%; height:300px;"></canvas></span>
  	<span><canvas id="cpuHistoryChart" style="width: 100%; height:300px;"></canvas></span>
<#if messages?size gt 0>
<#list messages>
<table><tbody><th>timestamp</th><th class="right">runtime</th><th class="right">(ms)</th><th>message</th></tbody>
<tbody><th class="monospaced pointer-cursor lime_background runtime" runtime="0">${startupTime?number_to_date?string("yyyy-MM-dd HH:mm:ss.SSS")}</th><th class="runtimeint right pointer-cursor lime_background monospaced runtime" runtime="0">${duration(0)}</th><th class="runtimems right pointer-cursor lime_background monospaced runtime" runtime="0">0</th><th runtime="0" class="lime_background">STARTUP</th></tbody>
<tbody>
<#items as msg>
<tr><td class="monospaced pointer-cursor runtime" runtime="${(msg.millis-startupTime)?c}">${(msg.timestamp).format("yyyy-MM-dd HH:mm:ss.SSS")}</td><td class="runtimeint right pointer-cursor monospaced runtime" runtime="${(msg.millis-startupTime)?c}">${duration(msg.millis-startupTime)}</td><td class="runtimems right pointer-cursor monospaced runtime" runtime="${(msg.millis-startupTime)?c}">&nbsp;${(msg.millis-startupTime)?c}</td><td class="pre-wrap monospaced" runtime="${(msg.millis-startupTime)?c}">${msg.value!'-'}</td></tr>
</#items>
</tbody></table>
<#else>
</#list>
</#if>
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
			plugins: {
				legend: {
					position: 'top',
				},
				title: {
					display: true,
					text: 'Memory usage'
				},
			},
		},
	});

	var chrt2 = $('#memHistoryChart');
      var chartId = new Chart(chrt2, {
         type: 'line',
         data: {
         	labels: [
         		<#list TS as ts>'${ts.format('HH:mm:ss')}',</#list>
         	],
            datasets: [
            {
               label: "MiB used",
               data: [<#list MEMORY as rep>'${(rep.USED/1024/1024)?round?c}',</#list>],
               backgroundColor: 'lightyellow',
               borderColor: 'yellow',
               fill: true,
            },
            {
               label: "MiB free",
               data: [<#list MEMORY as rep>'${(rep.FREE/1024/1024)?round?c}',</#list>],
               backgroundColor: 'lightgreen',
               borderColor: 'green',
               fill: true,
            },
            ]
         },
		options: {
			responsive: false,
			scales: {
				y: {
						stacked: true,
					}
				,
			},
			plugins: {
				legend: {
					position: 'top',
				},
				title: {
					display: true,
					text: 'Memory consumption in time'
				},
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

	var chrt3 = $('#cpuHistoryChart');
	var chartId = new Chart(chrt3, {
		type: 'line',
		data: {
			labels: [
				<#list TS as ts>'${ts.format('HH:mm:ss')}',</#list>
			],
			datasets: [
			<#list threads>
			<#items as t>
			{
				label: "${t.id?c}:${t.name} (<#if t.millis lt 0>X<#else>${t.millis?c!'?'}</#if>)",
				data: [<#list CPU as rep>'<#if rep[t.id?c+':'+t.name] lt 0>null<#else>${(rep[t.id?c+':'+t.name]?c)!'null'}</#if>',</#list>],
				spanGaps: false,
			},
			</#items>
			</#list>
			]
		},
		options: {
			responsive: false,
			plugins: {
				legend: {
					position: 'top',
				},
				title: {
					display: true,
					text: 'thread CPU time (ms per ${(msBetweenSamples/1000)?c}s  sample)'
				},
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
	
	$(".runtime").click(
	  function(event) {
	  	baseMs = event.target.getAttribute("runtime");
	  	$('.runtimeint').text(
	  	function(index, currentContent){
	  		resultMs = $('.runtimeint:eq('+index+')').attr('runtime') - baseMs;
	  		return formatDuration(resultMs);
	  	}
	  	);
	  	$('.runtimems').text(
	  	function(index, currentContent){
	  		resultMs = $('.runtimems:eq('+index+')').attr('runtime') - baseMs;
	  		if(resultMs<0)
	  			return resultMs;
	  		else
	  			return '\xa0' + resultMs;
	  	}
	  	);
	  	$("[runtime]").removeClass('lime_background');
	  	$("[runtime='"+baseMs+"']").addClass('lime_background');
	  }
	);
</script>

</@html.simpleDocument>
