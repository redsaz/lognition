# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security

## [0.1.2] - 2019-01-07
## Fixed
- [Issue #2](https://github.com/redsaz/lognition/issues/2) which prevented
  reviews recalculating logs when a review had a bad label selector.

## [0.1.1] - 2018-12-29
### Added
- Fix [Issue #5](https://github.com/redsaz/lognition/issues/5): Add error
  count and error percentage graphs to the log view and review view.

## [0.1.0] - 2018-12-28
### Changed
- Switch from Wildfly Swarm to Thorntail. It's the new name of the that
  project.
### Fixed
- Fix [Issue #4](https://github.com/redsaz/lognition/issues/4). If a test 
  has one data point, when viewing the test as part of a review, the
  review would return an error page with a 500 status. The reason:
  Different logs have different histogram datapoints and different
  percentile datapoints. These points need to line up on the x-axis, so
  the points are interpolated with each other. The interpolator we used
  requires at least two data points in order to interpolate. The fix was
  to add a special case for the single point and treat it as a constant
  across the entire range.
- Fix [Issue #3](https://github.com/redsaz/lognition/issues/3). The
  problem was in the label selector parser, specifically when an
  "exists"-style selector is used, followed by any other selector.

