import { InputBase, Paper } from '@material-ui/core'
import { Search as SearchIcon } from '@material-ui/icons'

import { SearchProps } from './SearchInterfaces'

import './Search.scss'

export const Search:React.FC<SearchProps> = (props: SearchProps) => {
  const { placeholder, handleChangeSearch } = props

  return (
    <Paper className='search__root'>
      <SearchIcon />
      <InputBase
        className='search__input'
        placeholder={placeholder}
        onChange={e => handleChangeSearch(e)}
      />
    </Paper>
  )
}
