# Default configs

!!! summary ""
    copyQualityConfigs task

Quality tools require direct access to configuration files, so default configs (inside plugin jar)
must be copied into temporary location.

It is also used for auto-generating configs (spotbugs exclusions).

!!! note
    In older plugin versions, default configs copying was implicit (it was done just before quality task execution 
    in doFirst block), which breake build cache. Also, it was completely not compatible
    with the configuration cache.
    
    Performing all configs-related actions in scope of one task allows proper caching of its result 
    (temp. configs directory)

Task is not intended to be used directly: all quality tasks depend on it. 