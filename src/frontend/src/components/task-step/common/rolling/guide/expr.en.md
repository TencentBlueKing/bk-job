# User Guide of Rolling Execute

### Introduction
By default, Job runs in parallel against all the hosts set in the `host(s)` field. If you wanna execute only a few hosts at a time, for example during a rolling update, you can switch `Rolling Exec.` on  and set the strategy to defind how many hosts Job should sent to execute at a single time.

### Strategy Expression
- Rules
	- Each position is separated by a `space`
	- Only `n` `n%` `*n` `+n` are allowed in each position, and `n` can only be an integer
	- `100%` is allowed only in the last position
	- Operators (`*` and `+`) are only allowed to appear before the number `n`
	- Equations (such as `+n` or `*n`) are only allowed to appear in the last digit
	- `0` is not allowed
- Usage
	- n
	  `n` must be an integer value representing a specific number of host(s)
	- n%
	  `n` must be an integer value, representing n percent of the total number of hosts (rounded upwards if float is encountered)
	- +n
	  Means an additional `n` hosts on the basis of previous batch each time
	- *n
	  Means multiply by `n` on the basis of previous batch each time

### Example
If you have 100 hosts to run:
- Specific units requirements, e.g., only 20 hosts per batch
	```
	Rolling strategy: 20
	Batch result: The first batch would contain 20 hosts, the second batch would contain 20 hosts, the third batch would contain 20 hosts, the fourth batch would contain 20 hosts, and the last batch would contain 20
	(The system will randomly choose 20 units from the total 100 hosts per batch, and will contain all the remaining hosts if fewer than 20 hosts remained)
	```
- As a percentage of the total, such as 30% of each batch
	```
	Rolling strategy: 30%
	Batch results: 30 hosts in the first batch, 30 hosts in the second batch, 30 hosts in the third batch, and 10 hosts in the fourth batch
	(Since the remaining hosts are less than 30 units by the fourth batch, the entire balance (i.e. 10 units) is acquired)
	```
- Batch-by-batch incremental, such as 2 times per batch (or specifically units)
  ```
  Rolling strategy: 5 *2
  Batch results: 5 hosts for the first batch, 10 hosts for the second batch, 20 hosts for the third batch, 40 hosts for the fourth batch, and 25 hosts for the fifth batch
  (the fifth batch should be 40*2=80 hosts, but the remaining host is not enough, so took the rest of all)
  
  Or
  
  Rolling strategy: 5 +10
  Batch results: 5 hosts in the first batch, 15 hosts in the second batch, 25 hosts in the third batch, 35 hosts in the fourth batch, 20 hosts in the fifth batch.
  (the fifth batch should be 35+10=45 hosts, but the remaining hosts are not enough, so took the rest of all)
  ```
- Clarify the number of batches required, such as no matter how many hosts on hand, 5 batches are divided
  ```
  Method 1
  Rolling strategy: 20%
  Strategy description: each batch takes 20 percent of the total, so the total batch must be 5 batches
	
  Method 2
  Rolling strategy: 10 20 20 20 100%
  Strategy description: 10 hosts in the first batch, 20 hosts in the second batch, 20 hosts in the third batch, 20 hosts in the fourth batch, the fifth batch is the rest of all
  (When the last bit of the expression is 100%, it means take the remaining amount of all hosts)
	```