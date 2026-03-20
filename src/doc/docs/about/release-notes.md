# 6.1.0 Release notes

* Support source sets with a capital letters in the name
* Tools update
* Global rules suppression

## Support source sets with a capital letters in the name

It was impossible to use plugins with source sets, containing capital letters, like `integrationTest` ([#134](https://github.com/xvik/gradle-quality-plugin/issues/134))

## Tools update

Default tool versions updated:

* Checkstyle 11.0.1 -> 12.3.1
* PMD 7.16 -> 7.22
* Codenarc 3.6.0 -> 3.7.0
* Spotbugs 4.9.4 -> 4.9.8

### Checkstyle

Currently, the latest checkstyle version is 13.3.0, but checkstyle 13 requires java 21.
So checkstyle 13 update was postponed to preserve current java 17 requirement.

Anyway, checkstyle 13 support in plugin was implemented:

- It will disable checkstyle 13 on java lower 21
- Auto fallback feature will enable checkstyle 12 on java below 21

You can set checkstyle 13 manually with `quality.checkstyleVersion='13.3.0'`

New checkstyle rules added into default config:

- [HexLiteralCase](https://checkstyle.org/checks/misc/hexliteralcase.html#HexLiteralCase)
- [TextBlockGoogleStyleFormatting](https://checkstyle.org/checks/coding/textblockgooglestyleformatting.html#TextBlockGoogleStyleFormatting)

!!! note
    Checkstyle 13 adds more new rules, they would be added into the default config in the next release. 

Auto fallback (which is not recommended to be used) could now disable incompatible rules automatically
(auto fallback used to test plugin).

## PMD

!!! warning
    PMD contains multiple fixes for existing rules, and, most likely, you'll see many warnings for old rules
    (not correctly working before). Not a problem, just be prepared. You can always downgrade pmd version manually
    with e.g. `quality.pmdVersion = '7.16.0'`

These rules were disabled (become too "greedy" now):

- [PublicMemberInNonPublicType](https://docs.pmd-code.org/pmd-doc-7.22.0/pmd_rules_java_design.html#publicmemberinnonpublictype)
- [AvoidCatchingGenericException](https://docs.pmd-code.org/pmd-doc-7.22.0/pmd_rules_java_errorprone.html#avoidcatchinggenericexception)

## Global rules suppression

It is now possible to disable rules without xml modifications. New options added:

```groovy
quality {
    suppressCheckstyleRules = [...]
    suppressPmdRules = [...]
    suppressCodenarcRules = [...]
    suppressSpotbugsRules = [...]
}
```

For example, if some checkstyle rule annoys you, you can disable it with:

```groovy
quality {
    suppressCheckstyleRules = ['ANNOYING_RULE', 'ANNOYING_RULE2']
}
```

!!! important
    This allows you to exclude rules, without manual xml modification and so without "owning" the config
    (especially useful, if you rely on default configs).

### How it works

When suppressions declared, plugin modifies xml config (default or user-provided) automatically
(under configs initialization task).

* Checkstyle requires all required modules to be declared: auto suppression removes required declarations from xml.
* PMD and Codenarc require you only to declare "modules" (blocks of rules) and manually exclude some rules within
module, if required. But, to do it, plugin have to read all rules declaration xmls from pmd or codenarc jars and
this might slightly slow down initialization.
* For spotbugs, suppressions are simply added into exclusions config (exclude.xml)
