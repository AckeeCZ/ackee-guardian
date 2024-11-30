# Releasing

If you release for the first time or you forgot the details, long version is recommended, which
explains the steps in a great detail. Otherwise you can use TLDR version.

## TLDR

1. Increase versions of the necessary library artifacts to be released, including the BOM version. 
    You can use `checkIfUpdateNeededSinceCurrentTag` Gradle task to help you figure out what 
    artifacts have changed since the last release. 
2. Update `CHANGELOG` for the new release.
3. Create a tag for the new BOM version in the required format.
4. Run `prePublishCheck` Gradle task and fix all found issues if needed.
5. If you were forced to publish more artifacts, update `CHANGELOG` and fast-forward the tag to the
    latest commit.
6. Push the tag. CI will perform necessary checks and publish all artifacts.

## Long version

Once you are ready to publish new versions of library artifacts, you can start publishing process:

1. First you need to increase the versions of all artifacts that need to be published, including the BOM version. 
    You can use `checkIfUpdateNeededSinceCurrentTag` Gradle task to figure out what modules have changed since the
    last release. It does not have to mean that everything needs to be released. For example if you
    did changes to both `datastore` and `jetpack`, the task will report changes in both, but you might
    want to release just `datastore` and keep the changes in `jetpack` for the future release. However,
    there are situations, when you will be forced to publish some updates, e.g. when you update `core-internal`.
    In this case you will need to publish new versions of all artifacts that depend on it to preserve
    binary compatibility between modules, because `core-internal` might contain breaking changes to public
    API. It might be difficult to ensure that proper artifacts are updated when necessary due to these
    kind of dependencies and that's why `verifyPublishing` task exists, that fails if there might be some
    potentially incompatible dependencies and forces you to publish relevant artifacts together in a single
    BOM.
2. Update `CHANGELOG` for the new release.
3. Create a tag for the new BOM version in the required format. You can optionally run `verifyBomVersion`
    Gradle task to verify, if the version of the BOM artifact is synced with the version in the tag.
4. Run `prePublishCheck` Gradle task. This task performs same checks as CI during a deployment, so you
    can fix issues faster by running this quicker local verification before pushing to the remote. 
    This task performs usual checks like building modules, running tests, etc., but it also
    runs all custom check tasks mentioned above, so you actually do not have to run them separately and
    you can just run `prePublishCheck`, but it is good to know that they exist and what they do. You
    can also find more detailed information in the documentation comments in the source code of those tasks.
5. If you were forced to publish more artifacts, update `CHANGELOG` and fast-forward the tag to the
   latest commit.
6. Push the tag. CI will perform necessary checks and publish all artifacts.

