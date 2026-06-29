<#--
  findProduct.ftl — Step 8: Product Search Screen
  Allows filtering by Product ID, Name, Category, Price, and Features.
  Results shown in a paginated table.
-->
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Find Product — ProductManagement</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h2 { color: #333; }
        .search-box { background: #f5f5f5; padding: 16px; border-radius: 6px; margin-bottom: 20px; }
        .search-box table { width: 100%; }
        .search-box td { padding: 6px 10px; }
        .search-box label { font-weight: bold; }
        .search-box input[type=text], .search-box input[type=number] {
            width: 200px; padding: 5px; border: 1px solid #ccc; border-radius: 3px;
        }
        .btn { padding: 7px 18px; background: #0073e6; color: #fff; border: none;
               border-radius: 4px; cursor: pointer; }
        .btn:hover { background: #005bb5; }
        .btn-secondary { background: #6c757d; }
        .results table { width: 100%; border-collapse: collapse; margin-top: 10px; }
        .results th { background: #0073e6; color: #fff; padding: 8px 12px; text-align: left; }
        .results td { padding: 8px 12px; border-bottom: 1px solid #ddd; }
        .results tr:hover { background: #f0f7ff; }
        .no-results { color: #999; padding: 20px 0; }
        .pagination { margin-top: 12px; }
        .pagination a { margin-right: 6px; padding: 5px 10px; border: 1px solid #ccc;
                        border-radius: 3px; text-decoration: none; color: #0073e6; }
        .pagination a.active { background: #0073e6; color: #fff; border-color: #0073e6; }
        .create-section { margin-bottom: 20px; }
    </style>
</head>
<body>

<h2>Find Product</h2>

<!-- Search Form -->
<div class="search-box">
    <form method="post" action="<@ofbizUrl>findProduct</@ofbizUrl>">
        <table>
            <tr>
                <td><label>Product ID</label></td>
                <td><input type="text" name="productId" value="${parameters.productId!}"/></td>
                <td><label>Product Name</label></td>
                <td><input type="text" name="productName" value="${parameters.productName!}"/></td>
            </tr>
            <tr>
                <td><label>Min Price</label></td>
                <td><input type="number" step="0.01" name="minPrice" value="${parameters.minPrice!}"/></td>
                <td><label>Max Price</label></td>
                <td><input type="number" step="0.01" name="maxPrice" value="${parameters.maxPrice!}"/></td>
            </tr>
            <tr>
                <td><label>Feature Type</label></td>
                <td><input type="text" name="productFeatureTypeId" placeholder="e.g. COLOR, SIZE"
                           value="${parameters.productFeatureTypeId!}"/></td>
                <td><label>Feature Description</label></td>
                <td><input type="text" name="featureDescription" value="${parameters.featureDescription!}"/></td>
            </tr>
            <tr>
                <td colspan="4" style="text-align:right; padding-top:10px;">
                    <button type="submit" class="btn">Search</button>
                    <a href="<@ofbizUrl>main</@ofbizUrl>" class="btn btn-secondary" style="text-decoration:none; margin-left:8px;">Reset</a>
                </td>
            </tr>
        </table>
    </form>
</div>

<!-- Create Product Button -->
<div class="create-section">
    <a href="<@ofbizUrl>createProduct</@ofbizUrl>" class="btn">+ Create New Product</a>
</div>

<!-- Search Results -->
<div class="results">
<#assign pageSize = 10/>
<#assign productList = productList![]/>
<#assign totalCount = productList?size/>

<#if totalCount == 0>
    <p class="no-results">No products found. Try adjusting your search criteria.</p>
<#else>
    <#assign currentPage = (parameters.page!1)?number/>
    <#assign startIndex = (currentPage - 1) * pageSize/>
    <#assign endIndex = (startIndex + pageSize - 1)?min(totalCount - 1)/>
    <#assign totalPages = ((totalCount - 1) / pageSize)?floor + 1/>

    <p>Showing ${startIndex + 1}–${endIndex + 1} of ${totalCount} results</p>

    <table>
        <thead>
            <tr>
                <th>Product ID</th>
                <th>Product Name</th>
                <th>Price</th>
                <th>Currency</th>
                <th>Feature Type</th>
                <th>Feature</th>
                <th>Category</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
        <#list productList[startIndex..endIndex] as product>
            <tr>
                <td>${product.productId!}</td>
                <td>${product.productName!}</td>
                <td>${product.price!}</td>
                <td>${product.currencyUomId!}</td>
                <td>${product.productFeatureTypeId!}</td>
                <td>${product.featureDescription!}</td>
                <td>${product.productCategoryId!}</td>
                <td>
                    <form method="post" action="<@ofbizUrl>updateProduct</@ofbizUrl>" style="display:inline;">
                        <input type="hidden" name="productId" value="${product.productId!}"/>
                        <input type="number" step="0.01" name="price" placeholder="New Price" style="width:90px;"/>
                        <button type="submit" class="btn" style="padding:4px 10px;">Update</button>
                    </form>
                </td>
            </tr>
        </#list>
        </tbody>
    </table>

    <!-- Pagination -->
    <#if totalPages gt 1>
    <div class="pagination">
        <#list 1..totalPages as p>
            <a href="<@ofbizUrl>findProduct?productId=${parameters.productId!}&productName=${parameters.productName!}&page=${p}</@ofbizUrl>"
               class="${(p == currentPage)?string('active', '')}">
                ${p}
            </a>
        </#list>
    </div>
    </#if>
</#if>
</div>

</body>
</html>
