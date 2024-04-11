<#function min(a, b)>
  <#return (a < b)?then(a, b)>
</#function>
<#function max(a, b)>
  <#return (a > b)?then(a, b)>
</#function>
<#function duration(ms)>
  <#assign sec=(ms/1000)?abs>
  <#assign h=(sec/3600)?floor>
  <#assign m=((sec%3600)/60)?floor>
  <#assign s=sec-(((sec/60)?floor)*60)>
  <#return (ms < 0)?then('-','+')+(h>0)?then(h?c+':'+m?string('00'),m?c)+':'+s?string('00.000')+' ('+ms?c+'ms)'>
</#function>
