<#if lastTestDriveList?has_content>
  <table border="1" style="width:100%">
    <tr>
      <td width="33%">Total No.</td>
      <td width="33%">Km In</td>
      <td width="33%">Km Out</td>
    </tr>
    <#assign testDriveCounter = 0>
    <#list lastTestDriveList as testDrive>
    <#assign testDriveCounter = testDriveCounter + 1>
      <tr>
        <td width="33%">Test Drive ${testDriveCounter}</td>
        <td width="33%">${testDrive.kmIn}</td>
        <td width="33%">${testDrive.kmOut}</td>
      </tr>
    </#list>
  </table>
  <#else>
     There is no Test drive record for this car.
</#if>

