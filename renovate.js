module.exports = {
  branchPrefix: 'renovate/',
  username: 'renovate-release',
  gitAuthor: 'Renovate Bot <bot@renovateapp.com>',
  onboarding: false,
  platform: 'github',
  includeForks: true,
  dryRun: 'full',
  packageRules: [
    {
      description: 'lockFileMaintenance',
      matchUpdateTypes: [
        'patch',
        'minor',
        'major'
      ],
      dependencyDashboardApproval: false,
      stabilityDays: 0,
    },
  ],
};