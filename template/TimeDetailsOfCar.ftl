<table border="1" style="width:100%">
  <tr>
     <td width="33%">Section</td>
     <td width="33%">In Time</td>
     <td width="33%">Out Time</td>
  </tr>
<#--================== Time Details of car ==================-->
 <#list lastStatusList as status>
  <#if status.carStatus != 'Ready for Delivered'>
    <tr>
      <td width="33%">${status.carStatus}</td>
      <td width="33%">${status.inTime}</td>
      <td width="33%">${status.outTime}</td>
    </tr>
  </#if>
 </#list>
</table>
