<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Customer Finder</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f0f2f5; }
        .container { max-width: 1200px; margin: 0 auto; }
        h2 { color: #2c3e50; border-bottom: 2px solid #4a6fa5; padding-bottom: 8px; }
        .card { background: #fff; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
        .form-row { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; margin-bottom: 10px; }
        .form-group { display: flex; flex-direction: column; gap: 4px; }
        .form-group label { font-size: 12px; font-weight: bold; color: #555; }
        .form-group input { padding: 7px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 13px; }
        .btn { padding: 8px 18px; border: none; border-radius: 4px; cursor: pointer; font-size: 13px; font-weight: bold; }
        .btn-primary  { background: #4a6fa5; color: #fff; }
        .btn-success  { background: #27ae60; color: #fff; }
        .btn-warning  { background: #e67e22; color: #fff; font-size: 11px; padding: 5px 10px; }
        table.results { width: 100%; border-collapse: collapse; font-size: 13px; }
        table.results th { background: #4a6fa5; color: #fff; padding: 10px 8px; text-align: left; }
        table.results td { border-bottom: 1px solid #e0e0e0; padding: 8px; vertical-align: middle; }
        table.results tr:hover { background: #f5f8ff; }
        .inline-form { display: flex; gap: 6px; align-items: center; }
        .inline-form input { padding: 4px 7px; border: 1px solid #ccc; border-radius: 3px; font-size: 12px; width: 120px; }
        .msg-error   { color: #c0392b; background: #fde8e8; padding: 10px 15px; border-radius: 4px; margin-bottom: 15px; }
        .msg-success { color: #1a7a41; background: #e8f8ee; padding: 10px 15px; border-radius: 4px; margin-bottom: 15px; }
        .pagination  { margin-top: 12px; display: flex; gap: 6px; }
        .pagination a { padding: 5px 12px; border: 1px solid #ccc; border-radius: 4px; text-decoration: none; color: #4a6fa5; }
        .pagination a.active { background: #4a6fa5; color: #fff; border-color: #4a6fa5; }
        details summary { cursor: pointer; color: #27ae60; font-weight: bold; font-size: 14px; padding: 5px 0; }
        .count-badge { background: #4a6fa5; color: #fff; border-radius: 12px; padding: 2px 10px; font-size: 12px; margin-left: 8px; }
    </style>
</head>
<body>
<div class="container">

<h2>Customer Finder</h2>

<#if errorMessageList?has_content>
    <div class="msg-error">
        <#list errorMessageList as err>${err}<br/></#list>
    </div>
</#if>
<#if successMessage?has_content>
    <div class="msg-success">${successMessage}</div>
</#if>

<div class="card">
    <strong>Search Customers</strong>
    <form method="post" action="<@ofbizUrl>findCustomer</@ofbizUrl>" style="margin-top:12px;">
        <div class="form-row">
            <div class="form-group">
                <label>Party ID</label>
                <input type="text" name="partyId" value="${parameters.partyId!''}"/>
            </div>
            <div class="form-group">
                <label>First Name</label>
                <input type="text" name="firstName" value="${parameters.firstName!''}"/>
            </div>
            <div class="form-group">
                <label>Last Name</label>
                <input type="text" name="lastName" value="${parameters.lastName!''}"/>
            </div>
            <div class="form-group">
                <label>Email</label>
                <input type="text" name="emailAddress" value="${parameters.emailAddress!''}"/>
            </div>
            <div class="form-group">
                <label>Phone</label>
                <input type="text" name="contactNumber" value="${parameters.contactNumber!''}"/>
            </div>
            <div class="form-group">
                <label>Address</label>
                <input type="text" name="address1" value="${parameters.address1!''}"/>
            </div>
            <div class="form-group">
                <label>&nbsp;</label>
                <button type="submit" class="btn btn-primary">Search</button>
            </div>
        </div>
    </form>
</div>

<div class="card">
    <details>
        <summary>+ Add New Customer</summary>
        <form method="post" action="<@ofbizUrl>createCustomer</@ofbizUrl>" style="margin-top:14px;">
            <div class="form-row">
                <div class="form-group">
                    <label>Email *</label>
                    <input type="email" name="emailAddress" required placeholder="unique identifier"/>
                </div>
                <div class="form-group">
                    <label>First Name *</label>
                    <input type="text" name="firstName" required/>
                </div>
                <div class="form-group">
                    <label>Last Name *</label>
                    <input type="text" name="lastName" required/>
                </div>
                <div class="form-group">
                    <label>Phone</label>
                    <input type="text" name="contactNumber"/>
                </div>
                <div class="form-group">
                    <label>Address</label>
                    <input type="text" name="address1"/>
                </div>
                <div class="form-group">
                    <label>City</label>
                    <input type="text" name="city"/>
                </div>
                <div class="form-group">
                    <label>Postal Code</label>
                    <input type="text" name="zipOrPostalCode"/>
                </div>
                <div class="form-group">
                    <label>&nbsp;</label>
                    <button type="submit" class="btn btn-success">Create</button>
                </div>
            </div>
        </form>
    </details>
</div>

<#assign pageSize = 10/>
<#assign page = (parameters.page?has_content)?then(parameters.page?number, 1)/>
<#assign offset = (page - 1) * pageSize/>

<#if customerList?has_content>
<div class="card">
    <div style="margin-bottom:12px;">
        Results <span class="count-badge">${customerList?size}</span>
    </div>

    <#assign totalPages = ((customerList?size - 1) / pageSize)?floor + 1/>

    <table class="results">
        <thead>
            <tr>
                <th>Party ID</th>
                <th>First Name</th>
                <th>Last Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Address</th>
                <th>City</th>
                <th>Postal Code</th>
                <th>Update</th>
            </tr>
        </thead>
        <tbody>
        <#list customerList as c>
            <#if c?index >= offset && c?index < offset + pageSize>
            <tr>
                <td>${c.partyId!''}</td>
                <td>${c.firstName!''}</td>
                <td>${c.lastName!''}</td>
                <td>${c.emailAddress!''}</td>
                <td>${c.contactNumber!''}</td>
                <td>${c.address1!''}</td>
                <td>${c.city!''}</td>
                <td>${c.zipOrPostalCode!''}</td>
                <td>
                    <form method="post" action="<@ofbizUrl>updateCustomer</@ofbizUrl>" class="inline-form">
                        <input type="hidden" name="emailAddress" value="${c.emailAddress!''}"/>
                        <input type="text" name="contactNumber" placeholder="phone"   value="${c.contactNumber!''}"/>
                        <input type="text" name="address1"      placeholder="address" value="${c.address1!''}"/>
                        <button type="submit" class="btn btn-warning">Update</button>
                    </form>
                </td>
            </tr>
            </#if>
        </#list>
        </tbody>
    </table>

    <#if totalPages gt 1>
    <div class="pagination">
        <#assign baseUrl = "findCustomer?partyId=${parameters.partyId!''}&firstName=${parameters.firstName!''}&lastName=${parameters.lastName!''}&emailAddress=${parameters.emailAddress!''}"/>
        <#list 1..totalPages as p>
            <a href="<@ofbizUrl>${baseUrl}&page=${p}</@ofbizUrl>"
               class="${(p == page)?then('active', '')}">${p}</a>
        </#list>
    </div>
    </#if>
</div>

<#elseif parameters.emailAddress?has_content || parameters.firstName?has_content || parameters.partyId?has_content>
<div class="card">
    <p style="color:#888;">No customers found matching your criteria.</p>
</div>
</#if>

</div><!-- /container -->
</body>
</html>
