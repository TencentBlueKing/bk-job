export const formatScriptTypeValue = (value) => {
  const key = String(value).toLowerCase();
  const typeMap = {
    shell: 1,
    bat: 2,
    perl: 3,
    python: 4,
    powershell: 5,
    sql: 6,
    1: 'Shell',
    2: 'Bat',
    3: 'Perl',
    4: 'Python',
    5: 'Powershell',
    6: 'SQL',
  };
  return typeMap[key] || key;
};
