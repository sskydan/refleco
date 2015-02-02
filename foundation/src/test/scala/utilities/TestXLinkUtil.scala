package utilities

import testbase.UnitSpec


class TestXLinkUtil extends UnitSpec {
  val type1 =
    <linkbase xmlns="http://www.xbrl.org/2003/linkbase" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.xbrl.org/2003/linkbase http://www.xbrl.org/2003/xbrl-linkbase-2003-12-31.xsd">
      <roleRef xlink:type="simple" xlink:href="http://www.xbrl.org/lrr/role/negated-2009-12-16.xsd#negatedLabel" roleURI="http://www.xbrl.org/2009/role/negatedLabel" />
      <labelLink xlink:type="extended" xlink:role="http://www.xbrl.org/2003/role/link">
        <loc xlink:type="locator" xlink:href="http://xbrl.fasb.org/us-gaap/2012/elts/us-gaap-2012-01-31.xsd#us-gaap_OperatingExpenses" xlink:label="us-gaap_OperatingExpenses" />
        <labelArc xlink:type="arc" xlink:arcrole="http://www.xbrl.org/2003/arcrole/concept-label" xlink:from="us-gaap_OperatingExpenses" xlink:to="us-gaap_OperatingExpenses_lbl" />
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/label" xlink:label="us-gaap_OperatingExpenses_lbl" xml:lang="en-US">Operating Expenses</label>
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/terseLabel" xlink:label="us-gaap_OperatingExpenses_lbl" xml:lang="en-US">Total operating expenses</label>
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/totalLabel" xlink:label="us-gaap_OperatingExpenses_lbl" xml:lang="en-US">Total operating costs and expenses</label>

        <labelArc xlink:type="arc" xlink:arcrole="http://www.xbrl.org/2003/arcrole/concept-label" xlink:from="us-gaap_DisclosureOfCompensationRelatedCostsSharebasedPaymentsAbstract" xlink:to="us-gaap_DisclosureOfCompensationRelatedCostsSharebasedPaymentsAbstract_lbl" />
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/label" xlink:label="us-gaap_DisclosureOfCompensationRelatedCostsSharebasedPaymentsAbstract_lbl" xml:lang="en-US">Share-Based Compensation [Abstract]</label>
        <loc xlink:type="locator" xlink:href="http://xbrl.fasb.org/us-gaap/2012/elts/us-gaap-2012-01-31.xsd#us-gaap_DisclosureOfCompensationRelatedCostsShareBasedPaymentsTextBlock" xlink:label="us-gaap_DisclosureOfCompensationRelatedCostsShareBasedPaymentsTextBlock" />
        <labelArc xlink:type="arc" xlink:arcrole="http://www.xbrl.org/2003/arcrole/concept-label" xlink:from="us-gaap_DisclosureOfCompensationRelatedCostsShareBasedPaymentsTextBlock" xlink:to="us-gaap_DisclosureOfCompensationRelatedCostsShareBasedPaymentsTextBlock_lbl" />
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/label" xlink:label="us-gaap_DisclosureOfCompensationRelatedCostsShareBasedPaymentsTextBlock_lbl" xml:lang="en-US">Disclosure Of Compensation Related Costs Share Based Payments Text Block</label>
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/terseLabel" xlink:label="us-gaap_DisclosureOfCompensationRelatedCostsShareBasedPaymentsTextBlock_lbl" xml:lang="en-US">Share-Based Compensation</label>
        <loc xlink:type="locator" xlink:href="http://xbrl.fasb.org/us-gaap/2012/elts/us-gaap-2012-01-31.xsd#us-gaap_NewAccountingPronouncementsAndChangesInAccountingPrinciplesAbstract" xlink:label="us-gaap_NewAccountingPronouncementsAndChangesInAccountingPrinciplesAbstract" />
        <labelArc xlink:type="arc" xlink:arcrole="http://www.xbrl.org/2003/arcrole/concept-label" xlink:from="us-gaap_NewAccountingPronouncementsAndChangesInAccountingPrinciplesAbstract" xlink:to="us-gaap_NewAccountingPronouncementsAndChangesInAccountingPrinciplesAbstract_lbl" />
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/label" xlink:label="us-gaap_NewAccountingPronouncementsAndChangesInAccountingPrinciplesAbstract_lbl" xml:lang="en-US">Recent Accounting Pronouncements [Abstract]</label>
      </labelLink>
    </linkbase>
    
  val type2 =
    <linkbase xmlns:xlink="http://www.w3.org/1999/xlink" >
      <labelLink xlink:type="extended" xlink:role="http://www.xbrl.org/2003/role/link">
        <loc xlink:type="locator" xlink:href="http://taxonomies.xbrl.us/us-gaap/2009/elts/us-gaap-2009-01-31.xsd#us-gaap_LiabilitiesCurrent" xlink:label="element71" />
        <label xlink:type="resource" xlink:label="label71" xlink:role="http://www.xbrl.org/2003/role/label" xml:lang="en-US" id="label_us-gaap_LiabilitiesCurrent_en-US">Liabilities Current</label>
        <labelArc xlink:type="arc" xlink:arcrole="http://www.xbrl.org/2003/arcrole/concept-label" xlink:from="element71" xlink:to="label71" />
      </labelLink>
      <labelLink xlink:type="extended" xlink:role="http://www.xbrl.org/2003/role/link">
        <loc xlink:type="locator" xlink:href="http://taxonomies.xbrl.us/us-gaap/2009/elts/us-gaap-2009-01-31.xsd#us-gaap_LiabilitiesCurrent" xlink:label="element72" />
        <label xlink:type="resource" xlink:label="label72" xlink:role="http://www.xbrl.org/2003/role/totalLabel" xml:lang="en-US" id="total_us-gaap_LiabilitiesCurrent_en-US">Total current liabilities</label>
        <labelArc xlink:type="arc" xlink:arcrole="http://www.xbrl.org/2003/arcrole/concept-label" xlink:from="element72" xlink:to="label72" />
      </labelLink>

      <labelLink>
        <loc xlink:type="locator" xlink:href="http://xbrl.fasb.org/us-gaap/2012/elts/us-gaap-2012-01-31.xsd#us-gaap_OperatingExpenses" xlink:label="us-gaap_OperatingExpenses" />
        <labelArc xlink:type="arc" xlink:arcrole="http://www.xbrl.org/2003/arcrole/concept-label" xlink:from="us-gaap_OperatingExpenses" xlink:to="us-gaap_OperatingExpenses_lbl" />
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/label" xlink:label="us-gaap_OperatingExpenses_lbl" xml:lang="en-US">Operating Expenses</label>
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/terseLabel" xlink:label="us-gaap_OperatingExpenses_lbl" xml:lang="en-US">Total operating expenses</label>
        <label xlink:type="resource" xlink:role="http://www.xbrl.org/2003/role/totalLabel" xlink:label="us-gaap_OperatingExpenses_lbl" xml:lang="en-US">Total operating costs and expenses</label>
      </labelLink>
    </linkbase>
  
  "Retrieve resource" should "find valid resource in simple structure" in {
    val expected = Some("Operating Expenses")
    val actual = XLinkUtil.resolveResource("us-gaap:OperatingExpenses", type1)
    assert(expected == actual)
  }
  
  it should "find valid resource in complex structure" in {
    val expected = Some("Liabilities Current")
    val actual = XLinkUtil.resolveResource("us-gaap:LiabilitiesCurrent", type2)
    assert(expected == actual)
  }
  
  "Retrieve resources" should "find valid resource in simple structure" in {
    val expected = Set("Operating Expenses", "Total operating expenses", "Total operating costs and expenses")
    val actual = XLinkUtil.resolveResources("us-gaap:OperatingExpenses", type1).toSet
    assert(expected == actual)
  }
  
  it should "find valid resource in complex structure" in {
    val expected = Set("Liabilities Current", "Total current liabilities")
    val actual = XLinkUtil.resolveResources("us-gaap:LiabilitiesCurrent", type2).toSet
    assert(expected == actual)
  }
  
}